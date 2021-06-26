package com.open.erp.openerp.dominio.venda.service;

import com.open.erp.openerp.AplicationConfigTest;
import com.open.erp.openerp.dominio.cliente.repository.ClienteRepository;
import com.open.erp.openerp.dominio.estoque.service.EstoqueService;
import com.open.erp.openerp.dominio.titulosreceber.model.TituloAReceber;
import com.open.erp.openerp.dominio.titulosreceber.service.TitulosReceberService;
import com.open.erp.openerp.dominio.venda.api.dto.VendaDto;
import com.open.erp.openerp.dominio.venda.model.ItemVenda;
import com.open.erp.openerp.dominio.venda.model.Venda;
import com.open.erp.openerp.dominio.venda.repository.VendaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.validation.ValidationException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@DisplayName("VendaServiceTest")
public class VendaServiceTest extends AplicationConfigTest {

    @MockBean
    private VendaRepository repository;

    @MockBean
    private EstoqueService estoqueService;

    @MockBean
    private ClienteRepository clienteRepository;

    @MockBean
    private TitulosReceberService titulosReceberService;

    @Autowired
    private VendaService vendaService;

    @Test
    @DisplayName("deve lançar uma execao porque nao é permitido venda com total zerado")
    public void deveLancarExceptionAoEfetuarVenda() {
        VendaDto.ItemVendaDto item = VendaDto.ItemVendaDto.builder()
                .produtoId("id")
                .valorUnitario(BigDecimal.ZERO)
                .quantidade(BigDecimal.ONE)
                .build();

        List<VendaDto.ItemVendaDto> itens = Collections.singletonList(item);
        VendaDto dto = VendaDto.builder()
                .itens(itens)
                .cliente(null)
                .valorRecebido(BigDecimal.ZERO)
                .mobile(true)
                .build();

        Assertions.assertThrows(ValidationException.class, () -> vendaService.efetuarVenda(dto), "Não é permitido venda com total zerado");
    }

    @Test
    @DisplayName("deve efetuar uma venda")
    public void deveEfetuarVenda() {
        VendaDto.ItemVendaDto item = VendaDto.ItemVendaDto.builder()
                .produtoId("id")
                .valorUnitario(BigDecimal.TEN)
                .quantidade(BigDecimal.ONE)
                .build();

        List<VendaDto.ItemVendaDto> itens = Collections.singletonList(item);
        VendaDto dto = VendaDto.builder()
                .itens(itens)
                .cliente(null)
                .valorRecebido(BigDecimal.TEN)
                .mobile(true)
                .build();

        Venda venda = Mockito.mock(Venda.class);
        Mockito.when(repository.save(ArgumentMatchers.any(Venda.class))).thenReturn(venda);

        vendaService.efetuarVenda(dto);

        Mockito.verify(clienteRepository, Mockito.never()).findById(ArgumentMatchers.anyString());
        Mockito.verify(titulosReceberService, Mockito.times(1)).gerarTituloAPartirVenda(ArgumentMatchers.eq(venda));
        Mockito.verify(repository, Mockito.times(1)).save(ArgumentMatchers.any(Venda.class));
        Mockito.verify(estoqueService, Mockito.times(1)).reduzirNoEstoque(ArgumentMatchers.anyString(), ArgumentMatchers.any(BigDecimal.class));
    }

    @Test
    @DisplayName("deve remover uma venda")
    public void deveRemoverVenda() {
        String vendaId = "id-mock";
        Venda venda = Mockito.mock(Venda.class);
        Mockito.when(venda.getValorRecebido()).thenReturn(BigDecimal.TEN);
        Mockito.when(venda.getItens()).thenReturn(Collections.emptyList());
        Mockito.when(venda.getValorTotal()).thenReturn(BigDecimal.TEN);
        Optional<Venda> vendaOptoOptional = Optional.of(venda);
        Mockito.when(repository.findById(ArgumentMatchers.eq(vendaId))).thenReturn(vendaOptoOptional);

        vendaService.removerVenda(vendaId);
        Mockito.verify(repository, Mockito.times(1)).delete(ArgumentMatchers.any(Venda.class));
        Mockito.verify(estoqueService, Mockito.never()).adicionarNoEstoque(ArgumentMatchers.any(), ArgumentMatchers.any());
        Mockito.verify(titulosReceberService, Mockito.never()).removerTitulo(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("deve remover uma venda e remover os titulos")
    public void deveRemoverVendaETitulos() {
        String vendaId = "id-mock";
        Venda venda = Mockito.mock(Venda.class);
        Mockito.when(venda.getValorRecebido()).thenReturn(BigDecimal.ZERO);
        Mockito.when(venda.getItens()).thenReturn(Collections.emptyList());
        Mockito.when(venda.getValorTotal()).thenReturn(BigDecimal.TEN);
        Optional<Venda> vendaOptoOptional = Optional.of(venda);
        Mockito.when(repository.findById(ArgumentMatchers.eq(vendaId))).thenReturn(vendaOptoOptional);

        TituloAReceber titulo = Mockito.mock(TituloAReceber.class);
        Mockito.when(titulosReceberService.getTituloByVenda(ArgumentMatchers.eq(vendaId))).thenReturn(titulo);

        vendaService.removerVenda(vendaId);
        Mockito.verify(repository, Mockito.times(1)).delete(ArgumentMatchers.any(Venda.class));
        Mockito.verify(titulosReceberService, Mockito.times(1)).removerTitulo(ArgumentMatchers.eq(titulo));
        Mockito.verify(estoqueService, Mockito.never()).adicionarNoEstoque(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    @DisplayName("deve remover uma venda e adicionar 3 itens no estoque")
    public void deveRemoverEAdicionarItensNoEstoque() {
        String vendaId = "id-mock";
        Venda venda = Mockito.mock(Venda.class);
        Mockito.when(venda.getValorRecebido()).thenReturn(BigDecimal.TEN);
        List<ItemVenda> itens = IntStream
                .range(0, 2)
                .mapToObj(value -> ItemVenda.builder()
                        .produtoId("qualquer")
                        .quantidade(BigDecimal.ONE)
                        .valorTotal(BigDecimal.TEN)
                        .build())
                .collect(Collectors.toList());


        Mockito.when(venda.getItens()).thenReturn(itens);
        Mockito.when(venda.getValorTotal()).thenReturn(BigDecimal.TEN);
        Optional<Venda> vendaOptoOptional = Optional.of(venda);
        Mockito.when(repository.findById(ArgumentMatchers.eq(vendaId))).thenReturn(vendaOptoOptional);

        vendaService.removerVenda(vendaId);
        Mockito.verify(repository, Mockito.times(1)).delete(ArgumentMatchers.any(Venda.class));
        Mockito.verify(estoqueService, Mockito.times(2)).adicionarNoEstoque(ArgumentMatchers.anyString(), ArgumentMatchers.any(BigDecimal.class));
        Mockito.verify(titulosReceberService, Mockito.never()).removerTitulo(ArgumentMatchers.any());
    }
}
