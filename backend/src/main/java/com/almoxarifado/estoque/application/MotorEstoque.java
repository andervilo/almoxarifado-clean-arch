package com.almoxarifado.estoque.application;

import java.util.*;
import java.math.*;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.almoxarifado.shared.application.*;
import com.almoxarifado.shared.domain.*;
import com.almoxarifado.estoque.domain.PosicaoEstoque;
import com.almoxarifado.estoque.events.SaldoAlterado;

@Service
public class MotorEstoque {
    private final EstoqueRepository repository;
    private final Auditoria auditoria;
    private final PublicadorEventos eventos;
    public MotorEstoque(EstoqueRepository repository, Auditoria auditoria, PublicadorEventos eventos) {
        this.repository=repository; this.auditoria=auditoria; this.eventos=eventos;
    }
    private RegraNegocioException erro(String codigo,String mensagem) { return new RegraNegocioException(codigo,mensagem); }
    private static String texto(String valor) { return valor==null?"":valor; }

    @Transactional
    public UUID executar(UUID org,String usuario,UUID operacao,ComandoEstoque comando) {
        Objects.requireNonNull(org); Objects.requireNonNull(operacao); Objects.requireNonNull(comando.itemId());
        if(usuario==null || usuario.isBlank() || comando.motivo()==null || comando.motivo().isBlank()) throw new IllegalArgumentException("Usuario e motivo obrigatorios");
        repository.bloquear(org,operacao);
        var existente=repository.assinatura(org,operacao);
        if(existente.isPresent()) {
            if(!existente.get().equals(comando.toString())) throw erro("IDEMPOTENCIA_CONFLITO","Chave ja utilizada com outros dados");
            return operacao;
        }
        repository.bloquear(org,comando.itemId());
        var tipo=comando.tipo();
        if(!Set.of("ENTRADA","SAIDA","TRANSFERENCIA","AJUSTE","ESTORNO","RESERVA","LIBERACAO","ENTREGA").contains(tipo)) throw new IllegalArgumentException("Tipo invalido");
        var item=tipo.equals("LIBERACAO")?null:repository.validarItem(org,comando.itemId());
        var posicoes=new ArrayList<>(repository.posicoes(org,comando.itemId()));
        var linhas=new ArrayList<EstoqueRepository.Linha>();
        var alteradas=new LinkedHashMap<UUID,PosicaoEstoque>();
        if(Set.of("ENTRADA","SAIDA","TRANSFERENCIA","AJUSTE","RESERVA").contains(tipo)) {
            var q=comando.quantidade();
            if(q==null || q.signum()==0 || q.scale()>4 || q.precision()-q.scale()>15 || (!tipo.equals("AJUSTE") && q.signum()<0)) throw new IllegalArgumentException("Quantidade invalida");
        }
        switch(tipo) {
            case "ENTRADA" -> {
                validarRastreabilidade(item.rastreabilidade(),comando);
                adicionar(org,comando,posicoes,alteradas,linhas,comando.destinoId(),texto(comando.lote()),texto(comando.serie()),comando.validade(),comando.quantidade(),comando.custo(),Instant.now());
            }
            case "SAIDA","TRANSFERENCIA","RESERVA" -> {
                repository.validarLocal(org,comando.origemId());
                if(tipo.equals("TRANSFERENCIA")) {
                    if(Objects.equals(comando.origemId(),comando.destinoId())) throw new IllegalArgumentException("Origem e destino iguais");
                    repository.validarLocal(org,comando.destinoId());
                }
                if(tipo.equals("RESERVA") && comando.referenciaId()==null) throw new IllegalArgumentException("Referencia obrigatoria");
                var elegiveis=posicoes.stream().filter(p->p.localId().equals(comando.origemId()))
                    .filter(p->texto(comando.lote()).isEmpty() || p.lote().equals(comando.lote()))
                    .filter(p->texto(comando.serie()).isEmpty() || p.serie().equals(comando.serie()))
                    .filter(p->p.validade()==null || !p.validade().isBefore(LocalDate.now(ZoneOffset.UTC)))
                    .filter(p->p.disponivel().signum()>0).toList();
                var total=elegiveis.stream().map(PosicaoEstoque::disponivel).reduce(BigDecimal.ZERO,BigDecimal::add);
                if(total.compareTo(comando.quantidade())<0) throw erro("SALDO_INSUFICIENTE","Saldo disponivel insuficiente ou lotes vencidos");
                var restante=comando.quantidade();
                for(var p:elegiveis) {
                    if(restante.signum()==0) break;
                    var retirada=restante.min(p.disponivel());
                    if(tipo.equals("RESERVA")) {
                        var novo=p.alterar(BigDecimal.ZERO,retirada,p.custo()); salvar(novo,alteradas);
                        repository.reservar(org,comando.referenciaId(),p.id(),retirada);
                    } else {
                        var custo=retirar(org,p,retirada,alteradas,linhas);
                        if(tipo.equals("TRANSFERENCIA")) adicionar(org,comando,posicoes,alteradas,linhas,comando.destinoId(),p.lote(),p.serie(),p.validade(),retirada,custo,p.recebidoEm());
                    }
                    restante=restante.subtract(retirada);
                }
            }
            case "AJUSTE" -> {
                validarRastreabilidade(item.rastreabilidade(),new ComandoEstoque("ENTRADA",comando.itemId(),null,comando.destinoId(),comando.quantidade().abs(),comando.lote(),comando.serie(),comando.validade(),comando.custo(),comando.motivo(),null));
                if(comando.quantidade().signum()>0) adicionar(org,comando,posicoes,alteradas,linhas,comando.destinoId(),texto(comando.lote()),texto(comando.serie()),comando.validade(),comando.quantidade(),comando.custo(),Instant.now());
                else {
                    var p=posicoes.stream().filter(v->v.localId().equals(comando.destinoId()) && v.lote().equals(texto(comando.lote())) && v.serie().equals(texto(comando.serie())))
                        .findFirst().orElseThrow(()->new RecursoNaoEncontradoException("Posicao nao encontrada"));
                    retirar(org,p,comando.quantidade().abs(),alteradas,linhas);
                }
            }
            case "LIBERACAO","ENTREGA" -> {
                if(comando.referenciaId()==null) throw new IllegalArgumentException("Referencia obrigatoria");
                var reservas=repository.reservas(org,comando.referenciaId());
                if(reservas.isEmpty()) throw erro("RESERVA_INEXISTENTE","Reserva ativa nao encontrada");
                for(var r:reservas) {
                    var p=posicoes.stream().filter(v->v.id().equals(r.posicaoId())).findFirst().orElseThrow(()->erro("RESERVA_INVALIDA","Reserva de outro item"));
                    var novo=p.alterar(BigDecimal.ZERO,r.quantidade().negate(),p.custo());
                    if(tipo.equals("ENTREGA")) {
                        p.validarSaida(LocalDate.now(ZoneOffset.UTC));
                        retirar(org,novo,r.quantidade(),alteradas,linhas);
                    } else salvar(novo,alteradas);
                }
                repository.concluirReserva(org,comando.referenciaId(),tipo.equals("ENTREGA")?"CONSUMIDA":"LIBERADA");
            }
            case "ESTORNO" -> {
                if(comando.referenciaId()==null) throw new IllegalArgumentException("Referencia obrigatoria");
                if(repository.estornado(org,comando.referenciaId())) throw erro("ESTORNO_EXISTENTE","Movimento ja estornado");
                var originais=repository.linhas(org,comando.referenciaId());
                if(originais.isEmpty()) throw new RecursoNaoEncontradoException("Movimento original nao encontrado ou sem alteracao de saldo");
                // Desfaz primeiro as entradas para permitir estorno de transferencia com numero de serie.
                for(var l:originais.stream().sorted(Comparator.comparing(EstoqueRepository.Linha::quantidade).reversed()).toList()) {
                    var p=alteradas.get(l.posicaoId());
                    if(p==null) p=posicoes.stream().filter(v->v.id().equals(l.posicaoId())).findFirst().orElseThrow(()->new RecursoNaoEncontradoException("Movimento de outro item"));
                    if(l.quantidade().signum()>0) retirar(org,p,l.quantidade(),alteradas,linhas);
                    else adicionar(org,comando,posicoes,alteradas,linhas,p.localId(),p.lote(),p.serie(),p.validade(),l.quantidade().negate(),l.custo(),p.recebidoEm());
                }
            }
            default -> throw new IllegalArgumentException("Tipo invalido");
        }
        repository.operacao(org,operacao,comando,usuario,linhas);
        if(tipo.equals("ESTORNO")) repository.marcarEstorno(org,comando.referenciaId(),operacao);
        auditoria.registrar(org,usuario,"ESTOQUE_"+tipo,operacao.toString());
        for(var p:alteradas.values()) eventos.publicar(new SaldoAlterado(UUID.randomUUID(),Instant.now(),org,1,p.id(),p.itemId(),p.localId(),p.quantidade(),p.reservado(),p.lote(),p.validade(),p.serie(),p.custo(),repository.revisao(org,p.id())));
        return operacao;
    }
    private void validarRastreabilidade(String rastreio,ComandoEstoque c) {
        boolean lote=!texto(c.lote()).isBlank(),serie=!texto(c.serie()).isBlank();
        boolean correto=switch(rastreio) {
            case "NENHUM" -> !lote && !serie && c.validade()==null;
            case "LOTE" -> lote && !serie && c.validade()==null;
            case "LOTE_VALIDADE" -> lote && !serie && c.validade()!=null;
            case "NUMERO_SERIE" -> !lote && serie && c.validade()==null && c.quantidade().compareTo(BigDecimal.ONE)==0;
            default -> false;
        };
        if(!correto) throw erro("RASTREABILIDADE_INVALIDA","Lote, validade, serie ou quantidade incompativeis com o item");
    }
    private void salvar(PosicaoEstoque p,Map<UUID,PosicaoEstoque> alteradas) { repository.salvar(p); alteradas.put(p.id(),p); }
    private BigDecimal retirar(UUID org,PosicaoEstoque p,BigDecimal q,Map<UUID,PosicaoEstoque> alteradas,List<EstoqueRepository.Linha> linhas) {
        var almox=repository.validarLocal(org,p.localId());
        var custo=repository.ajustarCusto(org,p.itemId(),almox,q.negate(),BigDecimal.ZERO);
        salvar(p.alterar(q.negate(),BigDecimal.ZERO,p.custo()),alteradas);
        linhas.add(new EstoqueRepository.Linha(p.id(),q.negate(),custo)); return custo;
    }
    private void adicionar(UUID org,ComandoEstoque c,List<PosicaoEstoque> posicoes,Map<UUID,PosicaoEstoque> alteradas,
        List<EstoqueRepository.Linha> linhas,UUID local,String lote,String serie,LocalDate validade,BigDecimal q,BigDecimal custo,Instant recebido) {
        if(custo==null || custo.signum()<0 || custo.scale()>6) throw new IllegalArgumentException("Custo invalido");
        if(!lote.isEmpty() && posicoes.stream().anyMatch(v->v.lote().equals(lote) && !Objects.equals(v.validade(),validade)))
            throw erro("VALIDADE_DIVERGENTE","Lote do item ja cadastrado com outra validade");
        var almox=repository.validarLocal(org,local);
        var p=posicoes.stream().filter(v->v.localId().equals(local) && v.lote().equals(lote) && v.serie().equals(serie)).findFirst().orElse(null);
        if(p!=null) p=alteradas.getOrDefault(p.id(),p);
        if(p==null) p=new PosicaoEstoque(UUID.randomUUID(),org,c.itemId(),local,lote,serie,validade,recebido,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO);
        if(!Objects.equals(p.validade(),validade)) throw erro("VALIDADE_DIVERGENTE","Lote ja cadastrado com outra validade");
        if(!serie.isEmpty() && p.quantidade().add(q).compareTo(BigDecimal.ONE)>0) throw erro("SERIE_DUPLICADA","Numero de serie ja em estoque");
        var total=p.quantidade().add(q);
        var media=p.quantidade().multiply(p.custo()).add(q.multiply(custo)).divide(total,6,RoundingMode.HALF_EVEN);
        repository.ajustarCusto(org,c.itemId(),almox,q,custo);
        var novo=p.alterar(q,BigDecimal.ZERO,media); salvar(novo,alteradas);
        if(posicoes.stream().noneMatch(v->v.id().equals(novo.id()))) posicoes.add(novo);
        linhas.add(new EstoqueRepository.Linha(p.id(),q,custo));
    }
}
