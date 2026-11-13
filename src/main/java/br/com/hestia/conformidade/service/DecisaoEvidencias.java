package br.com.hestia.conformidade.service;
import br.com.hestia.conformidade.model.*;
import java.util.*;
/** Regra 2.1: informação ausente nunca equivale a violação. */
public final class DecisaoEvidencias {
 public enum Estado { ATENDIDO, NAO_ATENDIDO, NAO_VERIFICADO }
 public static StatusConformidade decidir(Map<CriterioConformidade,Estado> criterios){
  for(var c:List.of(CriterioConformidade.IA_AUTORIZADA,CriterioConformidade.POLITICA_INTERNA,CriterioConformidade.LGPD))
   if(criterios.get(c)==Estado.NAO_ATENDIDO)return StatusConformidade.NAO_CONFORME;
  return Arrays.stream(CriterioConformidade.values()).allMatch(c->criterios.get(c)==Estado.ATENDIDO)?StatusConformidade.CONFORME:StatusConformidade.REQUER_REVISAO;
 }
 private DecisaoEvidencias(){}
}
