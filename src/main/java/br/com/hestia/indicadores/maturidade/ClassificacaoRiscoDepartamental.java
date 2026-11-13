package br.com.hestia.indicadores.maturidade;

import java.math.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Índice interno de risco do uso de IA; não é probabilidade de incidente. */
@Component
public class ClassificacaoRiscoDepartamental {
    public static final String VERSAO="risco-departamental-1.0";
    public static final String FORMULA="100 - (0.30 * governanca + 0.30 * usoResponsavel + 0.40 * seguranca)";
    public enum Faixa { BAIXO, MEDIO, ALTO }
    public record Evidencia(String fator, BigDecimal nota, String fonte, String versaoCalculo,
        BigDecimal numerador, BigDecimal denominador, long amostra, BigDecimal coberturaTecnica,
        BigDecimal coberturaClassificacao, boolean suficiente, String motivo, List<String> campos) {}
    public record Resultado(String versao, String estado, BigDecimal indice, Faixa classificacao,
        Faixa classificacaoIndicativa, boolean fragil, String formula, List<Evidencia> evidencias,
        List<String> pendencias, String aviso) {}
    public Resultado classificar(List<FatorCalculado> fatores) {
        Objects.requireNonNull(fatores);
        var pesos=new LinkedHashMap<String,BigDecimal>();pesos.put("governanca",new BigDecimal("0.30"));
        pesos.put("usoResponsavel",new BigDecimal("0.30"));pesos.put("seguranca",new BigDecimal("0.40"));
        var porCodigo=new HashMap<String,FatorCalculado>();
        for(var f:fatores)if(porCodigo.putIfAbsent(f.codigo(),f)!=null)throw new IllegalArgumentException("Fator duplicado");
        var evidencias=new ArrayList<Evidencia>();var pendencias=new ArrayList<String>();
        BigDecimal aderencia=BigDecimal.ZERO;boolean completo=true;
        var versoes=new HashSet<String>();var naturezas=new HashSet<String>();
        for(var entrada:pesos.entrySet()){
            var f=porCodigo.get(entrada.getKey());
            if(f==null||f.nota()==null){completo=false;pendencias.add(entrada.getKey()+": sem nota válida");continue;}
            if(f.nota().signum()<0||f.nota().compareTo(new BigDecimal("100"))>0)throw new IllegalArgumentException("Nota fora de 0 a 100");
            versoes.add(f.versaoCalculo());if(f.naturezaDados()!=null)naturezas.add(f.naturezaDados());
            boolean seguranca=f.codigo().equals("seguranca");
            boolean suficiente=f.elegivelGlobal()&&"CALCULADO".equals(f.estado())
                &&f.coberturaTecnica()!=null&&f.coberturaTecnica().compareTo(seguranca?BigDecimal.ONE:new BigDecimal("0.95"))>=0
                &&f.coberturaClassificacao()!=null&&f.coberturaClassificacao().compareTo(new BigDecimal("0.95"))>=0
                &&(seguranca?f.amostra()==CatalogoControlesSeguranca.Controle.values().length && Long.valueOf(CatalogoControlesSeguranca.Controle.values().length).equals(f.populacao()):f.amostra()>=30)
                &&f.fonte()!=null&&!f.fonte().isBlank()&&CalculadorasMaturidade.VERSAO.equals(f.versaoCalculo());
            if(!suficiente)pendencias.add(f.codigo()+": "+f.motivo());
            evidencias.add(new Evidencia(f.codigo(),f.nota(),f.fonte(),f.versaoCalculo(),f.numerador(),f.denominador(),
                f.amostra(),f.coberturaTecnica(),f.coberturaClassificacao(),suficiente,f.motivo(),f.campos()));
            aderencia=aderencia.add(f.nota().multiply(entrada.getValue()));
        }
        if(versoes.size()>1||naturezas.size()>1){completo=false;pendencias.add("Versões ou naturezas incompatíveis; não combinar fontes");}
        String aviso="Índice interno descritivo, não probabilidade de incidente nem certificação de segurança. Qualidade dos dados é separada da faixa de risco.";
        if(!completo)return new Resultado(VERSAO,"SEM_CLASSIFICACAO",null,null,null,true,FORMULA,List.copyOf(evidencias),List.copyOf(pendencias),aviso);
        BigDecimal indice=new BigDecimal("100").subtract(aderencia);
        Faixa faixa=indice.compareTo(new BigDecimal("25"))<0?Faixa.BAIXO:indice.compareTo(new BigDecimal("50"))<0?Faixa.MEDIO:Faixa.ALTO;
        boolean fragil=!pendencias.isEmpty();
        return new Resultado(VERSAO,fragil?"FRAGIL":"CLASSIFICADO",indice.setScale(2,RoundingMode.HALF_UP),fragil?null:faixa,faixa,fragil,FORMULA,List.copyOf(evidencias),List.copyOf(pendencias),aviso);
    }
}
