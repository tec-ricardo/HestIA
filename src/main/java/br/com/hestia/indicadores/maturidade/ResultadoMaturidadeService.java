package br.com.hestia.indicadores.maturidade;

import br.com.hestia.usuario.model.*;
import br.com.hestia.departamento.repository.DepartamentoRepository;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.indicadores.service.ComposicaoMaturidadeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class ResultadoMaturidadeService {
    public enum Natureza { SINTETICA, OPERACIONAL }
    public record Consulta(LocalDate inicio,LocalDate fim,Natureza fonte,String snapshot,Long departamentoId,String departamentoOrigem,String tipoIssue){}
    public record Resultado(Long empresaId,Long departamentoId,String empresaOrigem,String departamentoOrigem,
        LocalDate inicio,LocalDate fim,Instant apuradoEm,String hashSnapshot,Natureza naturezaDados,
        String fusoOrigem,String versaoPesos,String versaoCalculo,String versaoParametros,Map<String,String> parametros,
        BigDecimal scoreGlobal,BigDecimal scoreParcial,BigDecimal coberturaPesos,List<String> dimensoesAusentes,
        List<FatorCalculado> fatores,List<String> avisos){}
    private final ConfiguracaoMaturidade configuracao;private final AdaptadorSinteticoMaturidade sintetico;
    private final AdaptadorOperacionalMaturidade operacional;private final AcessoRespostaService acesso;
    private final DepartamentoRepository departamentos;private final ComposicaoMaturidadeService composicao;
    public ResultadoMaturidadeService(ConfiguracaoMaturidade configuracao,AdaptadorSinteticoMaturidade sintetico,
            AdaptadorOperacionalMaturidade operacional,AcessoRespostaService acesso,DepartamentoRepository departamentos,ComposicaoMaturidadeService composicao){
        this.configuracao=configuracao;this.sintetico=sintetico;this.operacional=operacional;this.acesso=acesso;this.departamentos=departamentos;this.composicao=composicao;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Resultado consultar(Usuario usuario,Consulta consulta){
        Long empresa=acesso.validarEmpresa(usuario,null);
        if(usuario.getPerfil()!=PerfilUsuario.ADMIN&&usuario.getPerfil()!=PerfilUsuario.GESTOR)throw erro(HttpStatus.FORBIDDEN,"Consulta de resultados restrita ao escopo autorizado de ADMIN/GESTOR");
        if(consulta==null||consulta.inicio()==null||consulta.fim()==null||consulta.fonte()==null||consulta.inicio().isAfter(consulta.fim()))throw erro(HttpStatus.BAD_REQUEST,"Início, fim e fonte são obrigatórios");
        Long departamento=consulta.departamentoId();String depOrigem=consulta.departamentoOrigem();
        if(usuario.getPerfil()==PerfilUsuario.GESTOR){
            var d=usuario.getDepartamento();
            if(d==null||d.getEmpresa()==null||!empresa.equals(d.getEmpresa().getId()))throw erro(HttpStatus.FORBIDDEN,"Gestor sem departamento autorizado");
            if(departamento!=null&&!departamento.equals(d.getId()))throw erro(HttpStatus.FORBIDDEN,"Departamento não autorizado");
            departamento=d.getId();
        }
        if(departamento!=null){
            departamentos.findById(departamento).filter(d->d.getEmpresa()!=null&&empresa.equals(d.getEmpresa().getId())).orElseThrow(()->erro(HttpStatus.FORBIDDEN,"Departamento não autorizado"));
        }
        List<FatorCalculado> fatores;String codigo=null,hash=null;Map<String,String> parametros=new LinkedHashMap<>();
        parametros.put("minimoAmostra","30");parametros.put("coberturaMinima","0.95");parametros.put("completudeFinOpsSeguranca","1.00");parametros.put("notaQualificadaCompetencias","80");
        parametros.put("statusRegra","PROPOSTA_NAO_HOMOLOGADA");
        if(consulta.fonte()==Natureza.SINTETICA){
            codigo=configuracao.getEmpresasOrigem().get(empresa);
            if(codigo==null||codigo.isBlank())throw erro(HttpStatus.CONFLICT,"Mapeamento autorizado empresa/origem não configurado no servidor");
            hash=consulta.snapshot();if(hash==null||!hash.matches("[0-9a-fA-F]{64}"))throw erro(HttpStatus.BAD_REQUEST,"Selecione um único hash de snapshot SHA-256");
            if(departamento!=null){
                String mapeado=configuracao.getDepartamentosOrigem().get(departamento);
                if(mapeado==null)throw erro(HttpStatus.CONFLICT,"Mapeamento autorizado do departamento ausente");
                if(depOrigem!=null&&!mapeado.equals(depOrigem))throw erro(HttpStatus.FORBIDDEN,"Departamento de origem não autorizado");
                depOrigem=mapeado;
            }
            String tipo=consulta.tipoIssue();if(tipo!=null&&(tipo.isBlank()||tipo.length()>100))throw erro(HttpStatus.BAD_REQUEST,"Tipo de issue inválido");
            String chaveMeta=empresa+":"+(depOrigem==null?"*":depOrigem)+":"+tipo;
            BigDecimal meta=configuracao.getMetasDias().get(chaveMeta);
            if("nao-configurada".equals(configuracao.getVersaoParametros()))meta=null;
            if(meta!=null&&meta.signum()<=0)throw erro(HttpStatus.CONFLICT,"Meta de produtividade configurada inválida");
            if(tipo!=null)parametros.put("tipoIssue",tipo);if(meta!=null)parametros.put("metaDias",meta.toPlainString());
            try {fatores=sintetico.calcular(new AdaptadorSinteticoMaturidade.Escopo(codigo,depOrigem,hash,consulta.inicio(),consulta.fim(),tipo,meta));}
            catch(IllegalStateException ex){throw erro(HttpStatus.UNPROCESSABLE_ENTITY,ex.getMessage());}
            catch(IllegalArgumentException ex){throw erro(HttpStatus.BAD_REQUEST,ex.getMessage());}
            catch(DataAccessException ex){throw erro(HttpStatus.SERVICE_UNAVAILABLE,"Staging indisponível; nenhum dado foi importado automaticamente");}
        }else {
            if(consulta.snapshot()!=null||depOrigem!=null)throw erro(HttpStatus.BAD_REQUEST,"Fonte operacional não aceita snapshot/identidade sintética");
            try{fatores=operacional.calcular(empresa,departamento,consulta.inicio(),consulta.fim());}
            catch(DataAccessException ex){throw erro(HttpStatus.SERVICE_UNAVAILABLE,"Fonte operacional indisponível");}
        }
        return compor(empresa,departamento,codigo,depOrigem,consulta,hash,configuracao.getVersaoParametros(),parametros,fatores,composicao.consultar().versaoRegra());
    }
    public static Resultado compor(Long empresa,Long departamento,String codigo,String depOrigem,Consulta consulta,String hash,String versaoParametros,
                                   Map<String,String> parametros,List<FatorCalculado> fatores,String versaoPesos){
        if(fatores.size()!=8||fatores.stream().map(FatorCalculado::codigo).distinct().count()!=8)throw new IllegalArgumentException("São necessários os oito fatores distintos");
        var pesosEsperados=new ComposicaoMaturidadeService().consultar().fatores().stream()
                .collect(java.util.stream.Collectors.toMap(x->x.codigo(),x->x.peso()));
        for(var fator:fatores) {
            var peso=pesosEsperados.get(fator.codigo());
            if(peso==null||peso.compareTo(fator.peso())!=0||fator.nota()!=null&&(fator.nota().signum()<0||fator.nota().compareTo(new BigDecimal("100"))>0))
                throw new IllegalArgumentException("Fatores, pesos ou notas incompatíveis com a composição");
        }
        BigDecimal somaPesos=fatores.stream().map(FatorCalculado::peso).reduce(BigDecimal.ZERO,BigDecimal::add);
        if(somaPesos.compareTo(BigDecimal.ONE)!=0)throw new IllegalArgumentException("Pesos inconsistentes");
        BigDecimal cobertura=BigDecimal.ZERO,total=BigDecimal.ZERO;List<String> ausentes=new ArrayList<>();
        for(var f:fatores){
            if(!CalculadorasMaturidade.VERSAO.equals(f.versaoCalculo()))throw new IllegalArgumentException("Versões de cálculo incompatíveis");
            if(f.elegivelGlobal()&&f.nota()!=null){cobertura=cobertura.add(f.peso());total=total.add(f.peso().multiply(f.nota()));}else ausentes.add(f.codigo());
        }
        BigDecimal global=ausentes.isEmpty()?total.setScale(2,RoundingMode.HALF_UP):null;
        BigDecimal parcial=cobertura.signum()==0?null:total.divide(cobertura,MathContext.DECIMAL128).setScale(2,RoundingMode.HALF_UP);
        var publicos=fatores.stream().map(f->new FatorCalculado(f.codigo(),f.peso(),f.nota()==null?null:f.nota().setScale(2,RoundingMode.HALF_UP),f.estado(),f.elegivelGlobal(),f.motivo(),
            f.coberturaTecnica(),f.coberturaClassificacao(),f.amostra(),f.populacao(),f.numerador(),f.denominador(),f.formula(),f.fonte(),f.estatisticas(),f.avisos(),f.versaoCalculo()).comNatureza(consulta.fonte().name())).toList();
        return new Resultado(empresa,departamento,codigo,depOrigem,consulta.inicio(),consulta.fim(),Instant.now(),hash,consulta.fonte(),consulta.fonte()==Natureza.SINTETICA?"NAO_DECLARADO":"LOCAL_OPERACIONAL_SEM_CONVERSAO_ENTRE_FONTES",
                versaoPesos,CalculadorasMaturidade.VERSAO,versaoParametros,Map.copyOf(parametros),global,parcial,cobertura,List.copyOf(ausentes),publicos,
                List.of("Cálculo experimental: proposta 0.1 ainda sujeita à validação do grupo","Score parcial é exploratório e não comparável ao global","Populações sintética e operacional são mantidas separadas"));
    }
    private static ResponseStatusException erro(HttpStatus s,String m){return new ResponseStatusException(s,m);}
}
