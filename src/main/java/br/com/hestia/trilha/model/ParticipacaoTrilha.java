package br.com.hestia.trilha.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
@Entity @Table(name="participacoes_trilha",uniqueConstraints=@UniqueConstraint(columnNames={"trilha_id","usuario_id"}))
public class ParticipacaoTrilha {
    public enum Status { NAO_INICIADA, EM_ANDAMENTO, CONCLUIDA }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="trilha_id") private Trilha trilha;
    @Column(name="usuario_id",nullable=false) private Long usuarioId;
    @Column(nullable=false) private Long empresaId;
    @Column(nullable=false) private LocalDateTime iniciadaEm;
    private LocalDateTime concluidaEm;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status=Status.NAO_INICIADA;
    @ElementCollection @CollectionTable(name="trilha_conclusoes",joinColumns=@JoinColumn(name="participacao_id"))
    @MapKeyColumn(name="microtreinamento_id") @Column(name="conclusao_id",nullable=false)
    private Map<Long,Long> conclusoes=new HashMap<>();
    protected ParticipacaoTrilha(){}
    public ParticipacaoTrilha(Trilha trilha,Long usuarioId){
        this.trilha=trilha;this.usuarioId=usuarioId;this.empresaId=trilha.getEmpresaId();this.iniciadaEm=LocalDateTime.now();
    }
    public void sincronizar(Map<Long,Long> vinculos, LocalDateTime fim){
        sincronizar(vinculos,fim,!vinculos.isEmpty());
    }
    public void sincronizar(Map<Long,Long> vinculos, LocalDateTime fim,boolean conteudoIniciado){
        conclusoes.clear();conclusoes.putAll(vinculos);
        status=vinculos.size()==trilha.getEtapas().size()?Status.CONCLUIDA:
                conteudoIniciado?Status.EM_ANDAMENTO:Status.NAO_INICIADA;
        concluidaEm=status==Status.CONCLUIDA?fim:null;
    }
    public Long getId(){return id;} public Trilha getTrilha(){return trilha;}
    public Long getUsuarioId(){return usuarioId;} public Long getEmpresaId(){return empresaId;}
    public LocalDateTime getIniciadaEm(){return iniciadaEm;} public LocalDateTime getConcluidaEm(){return concluidaEm;}
    public Status getStatus(){return status;} public Map<Long,Long> getConclusoes(){return Map.copyOf(conclusoes);}
}
