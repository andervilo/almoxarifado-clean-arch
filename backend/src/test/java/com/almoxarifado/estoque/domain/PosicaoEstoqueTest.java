package com.almoxarifado.estoque.domain;
import static org.assertj.core.api.Assertions.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;
import com.almoxarifado.shared.domain.RegraNegocioException;
class PosicaoEstoqueTest {
 private PosicaoEstoque posicao() { return new PosicaoEstoque(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"L","",LocalDate.now().minusDays(1),Instant.now(),new BigDecimal("10"),new BigDecimal("4"),BigDecimal.ONE); }
 @Test void saldoReservadoNaoPodeSerConsumido() {
  assertThat(posicao().disponivel()).isEqualByComparingTo("6");
  assertThatThrownBy(()->posicao().alterar(new BigDecimal("-7"),BigDecimal.ZERO,BigDecimal.ONE)).isInstanceOf(RegraNegocioException.class);
 }
 @Test void quantidadeEReservaNuncaFicamNegativas() {
  assertThatThrownBy(()->posicao().alterar(new BigDecimal("-11"),BigDecimal.ZERO,BigDecimal.ONE)).isInstanceOf(RegraNegocioException.class);
  assertThatThrownBy(()->posicao().alterar(BigDecimal.ZERO,new BigDecimal("-5"),BigDecimal.ONE)).isInstanceOf(RegraNegocioException.class);
 }
 @Test void loteVencidoFicaBloqueadoParaSaida() {
  assertThatThrownBy(()->posicao().validarSaida(LocalDate.now())).isInstanceOf(RegraNegocioException.class);
 }
}
