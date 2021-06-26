package com.open.erp.openerp.dominio.venda.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.open.erp.openerp.AplicationConfigTest;
import com.open.erp.openerp.dominio.produto.service.ProdutoService;
import com.open.erp.openerp.dominio.venda.api.dto.VendaDto;
import com.open.erp.openerp.dominio.venda.model.Venda;
import com.open.erp.openerp.dominio.venda.repository.VendaRepository;
import com.open.erp.openerp.dominio.venda.service.VendaService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("VendaControllerTest")
public class VendaControllerTest extends AplicationConfigTest {

    @MockBean
    private VendaService service;

    @MockBean
    private VendaRepository repository;

    @MockBean
    private ProdutoService produtoService;

    @Autowired
    private MockMvc mockMvc;

    private static final String PATH = "/venda";

    @Test
    @DisplayName("Deve fazer o POST de um venda")
    public void testPOST() throws Exception {
        VendaDto dto = VendaDto.builder().build();

        String json = new ObjectMapper().writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());

        verify(service, times(1)).efetuarVenda(any(VendaDto.class));
    }

    @Test
    @DisplayName("deve retornar uma lista de vendas")
    public void testeGetAll() throws Exception {
        Venda venda = Venda.builder()
                .id("id-mock")
                .build();
        List<Venda> vendas = Collections.singletonList(venda);

        when(repository.findAll()).thenReturn(vendas);

        MockHttpServletResponse response = mockMvc.perform(MockMvcRequestBuilders.get(PATH)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        String jsonEsperado = "[{\"id\":\"id-mock\",\"dataVenda\":null,\"valorTotal\":null,\"valorRecebido\":null,\"valorAReceber\":null,\"itens\":null,\"cliente\":null,\"mobile\":null}]";
        Assertions.assertEquals(jsonEsperado, response.getContentAsString());
    }

    @Test
    @DisplayName("deve retornar uma pagina de vendas")
    public void testGetPage() {
        Venda venda = Venda.builder()
                .id("id-mock")
                .build();
        List<Venda> vendas = Collections.singletonList(venda);

        when(repository.findAllByOrderByDataVendaDesc()).thenReturn(vendas);
    }
}
