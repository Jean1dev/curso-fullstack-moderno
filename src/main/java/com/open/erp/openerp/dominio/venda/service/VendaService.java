package com.open.erp.openerp.dominio.venda.service;

import com.open.erp.openerp.dominio.estoque.service.EstoqueService;
import com.open.erp.openerp.dominio.venda.api.dto.VendaDto;
import com.open.erp.openerp.dominio.venda.model.ItemVenda;
import com.open.erp.openerp.dominio.venda.model.Venda;
import com.open.erp.openerp.dominio.venda.repository.VendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VendaService {

    @Autowired
    private VendaRepository repository;

    @Autowired
    private EstoqueService estoqueService;

    public void efetuarVenda(VendaDto dto) {
        List<ItemVenda> itens = dto.getItens()
                .stream()
                .map(ItemVenda::fromDto)
                .collect(Collectors.toList());

        BigDecimal total = itens.stream()
                .map(item -> item.getValorTotal())
                .reduce(BigDecimal.ZERO, (somatorio, valorUnitario) -> somatorio.add(valorUnitario));

        itens.forEach(itemCompra -> estoqueService.reduzirNoEstoque(itemCompra.getProdutoId(), itemCompra.getQuantidade()));

        repository.save(Venda.builder()
                .valorTotal(total)
                .itens(itens)
                .dataVenda(LocalDate.now())
                .build());
    }
}