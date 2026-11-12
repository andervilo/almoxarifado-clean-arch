package com.almoxarifado.organizacao.domain;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
class ConfiguracaoOrganizacaoTest {
 @Test void rejeitaPrazosForaDoIntervalo() {
  assertThatThrownBy(()->new ConfiguracaoOrganizacao(0)).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->new ConfiguracaoOrganizacao(366)).isInstanceOf(IllegalArgumentException.class);
 }
}
