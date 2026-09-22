package br.com.hestia.configuracao.model;

import br.com.hestia.empresa.model.Empresa;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuracoes_empresa")
public class ConfiguracaoEmpresa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, unique = true) private Empresa empresa;
    @Column(name = "registrar_usos_automaticamente", nullable = false) private boolean registrarUsosAutomaticamente = true;
    @Column(name = "bloquear_ferramentas_nao_aprovadas", nullable = false) private boolean bloquearFerramentasNaoAprovadas = true;
    @Column(name = "limite_custo_mensal", precision = 15, scale = 2) private BigDecimal limiteCustoMensal;
    @Column(name = "dias_retencao_auditoria", nullable = false) private int diasRetencaoAuditoria = 365;
    @Column(name = "atualizada_em", nullable = false) private LocalDateTime atualizadaEm;

    protected ConfiguracaoEmpresa() {}
    public ConfiguracaoEmpresa(Empresa empresa) { this.empresa = empresa; }
    public void atualizar(boolean registrar, boolean bloquear, BigDecimal limite, int dias) {
        this.registrarUsosAutomaticamente = registrar; this.bloquearFerramentasNaoAprovadas = bloquear;
        this.limiteCustoMensal = limite; this.diasRetencaoAuditoria = dias; this.atualizadaEm = LocalDateTime.now();
    }
    @PrePersist void prePersist() { if (atualizadaEm == null) atualizadaEm = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public boolean isRegistrarUsosAutomaticamente() { return registrarUsosAutomaticamente; }
    public boolean isBloquearFerramentasNaoAprovadas() { return bloquearFerramentasNaoAprovadas; }
    public BigDecimal getLimiteCustoMensal() { return limiteCustoMensal; }
    public int getDiasRetencaoAuditoria() { return diasRetencaoAuditoria; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
}
