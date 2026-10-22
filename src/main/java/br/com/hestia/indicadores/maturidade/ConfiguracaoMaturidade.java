package br.com.hestia.indicadores.maturidade;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;

/** Configuração de servidor; nunca derivada de IDs fornecidos pelo navegador. */
@Component
@ConfigurationProperties(prefix="hestia.maturidade")
public class ConfiguracaoMaturidade {
    private Map<Long,String> empresasOrigem=new HashMap<>();
    private Map<Long,String> departamentosOrigem=new HashMap<>();
    private Map<String,BigDecimal> metasDias=new HashMap<>();
    private String versaoParametros="nao-configurada";
    public Map<Long,String> getEmpresasOrigem(){return empresasOrigem;}
    public void setEmpresasOrigem(Map<Long,String> v){empresasOrigem=v;}
    public Map<Long,String> getDepartamentosOrigem(){return departamentosOrigem;}
    public void setDepartamentosOrigem(Map<Long,String> v){departamentosOrigem=v;}
    public Map<String,BigDecimal> getMetasDias(){return metasDias;}
    public void setMetasDias(Map<String,BigDecimal> v){metasDias=v;}
    public String getVersaoParametros(){return versaoParametros;}
    public void setVersaoParametros(String v){versaoParametros=v;}
}
