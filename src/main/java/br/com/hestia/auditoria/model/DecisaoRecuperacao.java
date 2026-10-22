package br.com.hestia.auditoria.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="decisoes_recuperacao")
public class DecisaoRecuperacao {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long empresaId;
    @Column(nullable=false) private Long usuarioId;
    private Long respostaId;
    @Column(nullable=false,length=80) private String recurso;
    @Column(nullable=false,length=40) private String decisao;
    @Column(nullable=false,length=100) private String motivo;
    @Column(nullable=false,length=50) private String regraVersao;
    @Column(nullable=false) private LocalDateTime registradaEm;
    protected DecisaoRecuperacao(){}
    public DecisaoRecuperacao(Long empresa,Long usuario,Long resposta,String recurso,String decisao,String motivo){this.empresaId=empresa;this.usuarioId=usuario;this.respostaId=resposta;this.recurso=recurso;this.decisao=decisao;this.motivo=motivo;this.regraVersao="autorizacao-elegibilidade-2";this.registradaEm=LocalDateTime.now();}
}
