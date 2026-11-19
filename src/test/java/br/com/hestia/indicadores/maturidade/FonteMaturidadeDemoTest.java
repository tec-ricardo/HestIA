package br.com.hestia.indicadores.maturidade;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class FonteMaturidadeDemoTest {
    JdbcTemplate jdbc;FonteMaturidadeDemo fonte;SingleConnectionDataSource ds;
    LocalDate inicio=LocalDate.of(2026,10,1),fim=LocalDate.of(2026,10,31);
    @BeforeEach void preparar(){
        ds=new SingleConnectionDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1","sa","",true);
        jdbc=new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE empresas(id BIGINT PRIMARY KEY,cnpj VARCHAR(20))");
        jdbc.execute("CREATE TABLE departamentos(id BIGINT PRIMARY KEY,empresa_id BIGINT REFERENCES empresas(id))");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V31__observacoes_maturidade_demo.sql")).execute(ds);
        jdbc.update("INSERT INTO empresas VALUES(1,'99999999000191'),(2,'12345678000199')");
        jdbc.update("INSERT INTO departamentos VALUES(10,1),(11,1),(20,2)");
        fonte=new FonteMaturidadeDemo(jdbc,new CalculadorasMaturidade());
        for(long dep:List.of(10L,11L)){
            for(int i=1;i<=36;i++)add(dep,"TREINAMENTO",Map.of("pessoa",dep+":"+i,"nota",80));
            for(int i=1;i<=60;i++)add(dep,"USO",Map.of("autorizacao",i<=52?"APROVADA":"RETIRADA","conformidade",i<=50?"CONFORME":"NAO_CONFORME"));
            for(int i=1;i<=30;i++){
                add(dep,"ISSUE",Map.of("tipo","TAREFA","duracaoDias",5,"metaDias",5));
                add(dep,"PR",Map.of("numero",i,"revisaoIndependente",i<=20));
            }
            add(dep,"FINANCEIRO",Map.of("brl",7164.56,"usd",17.53,"brlPorUsd",5,"orcamentoBrl",6357.74,"moedaOrcamento","BRL","mesCompleto",true));
            for(int i=1;i<=8;i++)add(dep,"CONTROLE",Map.of("controle","SEG0"+i,"resultado",i<=6?"ATENDIDO":"NAO_ATENDIDO"));
        }
    }
    @AfterEach void fechar(){ds.destroy();}
    void add(long dep,String tipo,Map<String,Object> dados){
        try{jdbc.update("INSERT INTO maturidade_observacoes_demo(empresa_id,departamento_id,versao,chave,tipo,data_referencia,natureza,dados) VALUES(1,?,?,?,?,?,'SIMULADA',?)",dep,CargaMaturidadeDemo.VERSAO,UUID.randomUUID().toString(),tipo,tipo.equals("FINANCEIRO")?fim:inicio.plusDays(14),new ObjectMapper().writeValueAsString(dados));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    @Test void calculaOsOitoIndicadoresOriginaisDoBanco(){
        var fatores=fonte.calcular(1L,null,inicio,fim);
        assertThat(fatores).hasSize(8).allMatch(f->f.nota()!=null&&f.elegivelGlobal());
        assertThat(fatores).allMatch(f->f.fonte().contains("PostgreSQL")&&f.naturezaDados().equals("DEMO"));
        var fin=fatores.stream().filter(f->f.codigo().equals("finOps")).findFirst().orElseThrow();
        assertThat(fin.nota()).isCloseTo(new java.math.BigDecimal("85.93"),within(new java.math.BigDecimal("0.005")));
        var seguranca=fatores.stream().filter(f->f.codigo().equals("seguranca")).findFirst().orElseThrow();
        assertThat(seguranca.nota()).isEqualByComparingTo("75");
        var resultado=ResultadoMaturidadeService.compor(1L,null,null,null,
            new ResultadoMaturidadeService.Consulta(inicio,fim,ResultadoMaturidadeService.Natureza.DEMO,null,null,null,null),null,
            CargaMaturidadeDemo.VERSAO,Map.of(),fatores,"teste");
        assertThat(resultado.scoreGlobal()).isNotNull();assertThat(resultado.coberturaPesos()).isEqualByComparingTo("1");
        assertThat(resultado.dimensoesAusentes()).isEmpty();
        assertThat(new ClassificacaoRiscoDepartamental().classificar(fonte.calcular(1L,10L,inicio,fim)).estado()).isEqualTo("CLASSIFICADO");
    }
    @Test void preservaEscopoDeEmpresaDepartamentoEPeriodo(){
        assertThat(fonte.calcular(1L,10L,inicio,fim).stream().filter(f->f.codigo().equals("aiLiteracy")).findFirst().orElseThrow().amostra()).isEqualTo(36);
        assertThatThrownBy(()->fonte.calcular(2L,null,inicio,fim)).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(403));
        assertThatThrownBy(()->fonte.calcular(1L,20L,inicio,fim)).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(403));
        assertThat(fonte.calcular(1L,null,inicio.minusYears(1),fim.minusYears(1))).allMatch(f->f.nota()==null&&!f.elegivelGlobal());
    }
    @Test void retirarObservacoesDoBancoRetiraANotaSemFallback(){
        jdbc.update("DELETE FROM maturidade_observacoes_demo WHERE tipo='CONTROLE'");
        assertThat(fonte.calcular(1L,null,inicio,fim).stream().filter(f->f.codigo().equals("seguranca")).findFirst().orElseThrow().nota()).isNull();
    }
    @Test void mesParcialNaoPresumeCompletudeFinanceira(){
        assertThat(fonte.calcular(1L,null,inicio.plusDays(1),fim).stream().filter(f->f.codigo().equals("finOps")).findFirst().orElseThrow().nota()).isNull();
    }
}
