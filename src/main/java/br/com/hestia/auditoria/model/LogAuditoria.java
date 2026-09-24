package br.com.hestia.auditoria.model;

import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.usuario.model.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "logs_auditoria", indexes = {
        @Index(name = "idx_auditoria_empresa_data", columnList = "empresa_id,data_hora"),
        @Index(name = "idx_auditoria_usuario", columnList = "usuario_id")
})
public class LogAuditoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "empresa_id")
    private Empresa empresa;
    @Column(nullable = false, length = 20) private String metodo;
    @Column(nullable = false, length = 500) private String recurso;
    @Column(nullable = false, length = 40) private String acao;
    @Column(name = "status_http", nullable = false) private Integer statusHttp;
    @Column(name = "endereco_ip", length = 64) private String enderecoIp;
    @Column(name = "data_hora", nullable = false) private LocalDateTime dataHora;

    protected LogAuditoria() {}
    public LogAuditoria(Usuario usuario, String metodo, String recurso, String acao,
                        Integer statusHttp, String enderecoIp) {
        this.usuario = usuario;
        this.empresa = usuario == null ? null : usuario.getEmpresa();
        this.metodo = metodo; this.recurso = recurso; this.acao = acao;
        this.statusHttp = statusHttp; this.enderecoIp = enderecoIp; this.dataHora = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Empresa getEmpresa() { return empresa; }
    public String getMetodo() { return metodo; }
    public String getRecurso() { return recurso; }
    public String getAcao() { return acao; }
    public Integer getStatusHttp() { return statusHttp; }
    public String getEnderecoIp() { return enderecoIp; }
    public LocalDateTime getDataHora() { return dataHora; }
}
