package br.com.hestia.departamento.model;

import br.com.hestia.empresa.model.Empresa;
import jakarta.persistence.*;

@Entity
@Table(name = "departamentos")
public class Departamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String responsavel;

    private String estruturaHierarquica;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    public Departamento() {
    }

    public Departamento(Long id, String nome, String responsavel,
                        String estruturaHierarquica, Empresa empresa) {
        this.id = id;
        this.nome = nome;
        this.responsavel = responsavel;
        this.estruturaHierarquica = estruturaHierarquica;
        this.empresa = empresa;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    public String getEstruturaHierarquica() {
        return estruturaHierarquica;
    }

    public void setEstruturaHierarquica(String estruturaHierarquica) {
        this.estruturaHierarquica = estruturaHierarquica;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }
}