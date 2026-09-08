package br.com.hestia.microtreinamento.model;

public class Microtreinamento {

    private Long id;
    private String titulo;
    private String descricao;
    private String conteudo;
    private TemaTreinamento tema;
    private Integer duracaoMinutos;
    private boolean ativo;

    public Microtreinamento() {
    }

    public Microtreinamento(
            Long id,
            String titulo,
            String descricao,
            String conteudo,
            TemaTreinamento tema,
            Integer duracaoMinutos,
            boolean ativo
    ) {
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.conteudo = conteudo;
        this.tema = tema;
        this.duracaoMinutos = duracaoMinutos;
        this.ativo = ativo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public TemaTreinamento getTema() {
        return tema;
    }

    public void setTema(TemaTreinamento tema) {
        this.tema = tema;
    }

    public Integer getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public void setDuracaoMinutos(Integer duracaoMinutos) {
        this.duracaoMinutos = duracaoMinutos;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}