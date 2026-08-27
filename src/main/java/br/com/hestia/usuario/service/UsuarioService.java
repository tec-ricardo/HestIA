package br.com.hestia.usuario.service;

import br.com.hestia.departamento.model.Departamento;
import br.com.hestia.departamento.repository.DepartamentoRepository;
import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.empresa.repository.EmpresaRepository;
import br.com.hestia.usuario.dto.UsuarioDTO;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final DepartamentoRepository departamentoRepository;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            DepartamentoRepository departamentoRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.departamentoRepository = departamentoRepository;
    }

    public Usuario criar(UsuarioDTO dto) {

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException(
                    "Já existe um usuário cadastrado com esse e-mail."
            );
        }

        Empresa empresa = empresaRepository.findById(dto.getEmpresaId())
                .orElseThrow(() ->
                        new RuntimeException("Empresa não encontrada.")
                );

        Departamento departamento =
                departamentoRepository.findById(dto.getDepartamentoId())
                        .orElseThrow(() ->
                                new RuntimeException("Departamento não encontrado.")
                        );

        if (!departamento.getEmpresa().getId().equals(empresa.getId())) {
            throw new RuntimeException(
                    "O departamento informado não pertence à empresa selecionada."
            );
        }

        Usuario usuario = new Usuario();

        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(dto.getSenha());
        usuario.setCargo(dto.getCargo());
        usuario.setPerfil(dto.getPerfil());
        usuario.setEmpresa(empresa);
        usuario.setDepartamento(departamento);

        if (dto.getAtivo() != null) {
            usuario.setAtivo(dto.getAtivo());
        }

        return usuarioRepository.save(usuario);
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado.")
                );
    }

    public Usuario atualizar(Long id, UsuarioDTO dto) {

        Usuario usuario = buscarPorId(id);

        Empresa empresa = empresaRepository.findById(dto.getEmpresaId())
                .orElseThrow(() ->
                        new RuntimeException("Empresa não encontrada.")
                );

        Departamento departamento =
                departamentoRepository.findById(dto.getDepartamentoId())
                        .orElseThrow(() ->
                                new RuntimeException("Departamento não encontrado.")
                        );

        if (!departamento.getEmpresa().getId().equals(empresa.getId())) {
            throw new RuntimeException(
                    "O departamento informado não pertence à empresa selecionada."
            );
        }

        usuario.setNome(dto.getNome());
        usuario.setCargo(dto.getCargo());
        usuario.setPerfil(dto.getPerfil());
        usuario.setEmpresa(empresa);
        usuario.setDepartamento(departamento);

        if (dto.getAtivo() != null) {
            usuario.setAtivo(dto.getAtivo());
        }

        return usuarioRepository.save(usuario);
    }

    public void excluir(Long id) {

        Usuario usuario = buscarPorId(id);

        usuarioRepository.delete(usuario);
    }
}