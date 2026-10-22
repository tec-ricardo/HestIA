package br.com.hestia.indicadores.maturidade;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.*;
import java.sql.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class SnapshotMaturidadeRepositoryTest {
    @Test void filtroDeSnapshotEEmpresaEhAplicadoNoBancoAntesDaLeitura() throws Exception {
        JdbcTemplate jdbc=mock(JdbcTemplate.class);Connection connection=mock(Connection.class);PreparedStatement st=mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(st);
        doAnswer(call->{call.<PreparedStatementCreator>getArgument(0).createPreparedStatement(connection);return null;})
            .when(jdbc).query(any(PreparedStatementCreator.class),any(RowCallbackHandler.class));
        new SnapshotMaturidadeRepository(jdbc).ler("training_records","A".repeat(64),"C09",r->{});
        verify(connection).prepareStatement(argThat(sql->sql.contains("l.origem_sha256 = ?")&&sql.contains("l.dados->>'company_id' = ?")));
        verify(st).setString(1,"A".repeat(64));verify(st).setString(2,"training_records");verify(st).setString(3,"C09");verify(st).setFetchSize(1000);
    }
    @Test void fatosDeProjetoExigemJoinNaMesmaEmpresaESnapshot() throws Exception {
        JdbcTemplate jdbc=mock(JdbcTemplate.class);Connection con=mock(Connection.class);PreparedStatement st=mock(PreparedStatement.class);when(con.prepareStatement(anyString())).thenReturn(st);
        doAnswer(call->{call.<PreparedStatementCreator>getArgument(0).createPreparedStatement(con);return null;}).when(jdbc).query(any(PreparedStatementCreator.class),any(RowCallbackHandler.class));
        new SnapshotMaturidadeRepository(jdbc).ler("pull_requests","B".repeat(64),"C02",r->{});
        verify(con).prepareStatement(argThat(sql->sql.contains("p.origem_sha256 = l.origem_sha256")&&sql.contains("p.dados->>'company_id' = ?")&&sql.contains("p.dados->>'project_id' = l.dados->>'project_id'")));
        verify(st).setString(3,"C02");
    }
    @Test void nomeDeTabelaNaoPodeVirLivrementeDaEntrada(){
        var jdbc=mock(JdbcTemplate.class);
        assertThatThrownBy(()->new SnapshotMaturidadeRepository(jdbc).ler("usuarios; DROP TABLE empresas","hash","C01",r->{})).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(jdbc);
    }
}
