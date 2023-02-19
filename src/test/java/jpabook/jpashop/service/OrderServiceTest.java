package jpabook.jpashop.service;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.exception.NotEnoughStockException;
import jpabook.jpashop.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrderServiceTest {

    @Autowired EntityManager em;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;

    @Test
    public void 상품주문() throws Exception {
        // given
        Member member = createMember();

        Book book = createBook("시골 JPA", 10000, 10);
        int orderCount = 2;
        
        // when
        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);

        // then
        Order foundOrder = orderRepository.findOne(orderId);
        assertThat(foundOrder.getStatus().equals(OrderStatus.ORDER));
        assertThat(foundOrder.getOrderItems().size() == 1);
        assertThat(foundOrder.getTotalPrice() == 10000 * orderCount);
        assertThat(book.getStockQuantity() == 8);
    }

    private Book createBook(String name, int price, int stockQuantity) {
        Book book = new Book();
        book.setName(name);
        book.setPrice(price);
        book.setStockQuantity(stockQuantity);
        em.persist(book);
        return book;
    }

    private Member createMember() {
        Member member = new Member();
        member.setName("회원1");
        member.setAddress(new Address("서울", "강가", "123-123"));
        em.persist(member);
        return member;
    }

    @Test
    public void 상품주문_재고수량초과() throws Exception {
        // given
        Member member = createMember();
        Book item = createBook("시골 JPA", 10000, 10);
        int orderCount = 11;

        // when
        assertThatThrownBy(() -> orderService.order(member.getId(), item.getId(), orderCount))
                .isInstanceOf(NotEnoughStockException.class); // then
    }

    @Test
    public void 주문취소() throws Exception {
        // given
        Member member = createMember();
        Book item = createBook("시골 JPA", 10000, 10);
        int orderCount = 2;

        // when
        Long orderId = orderService.order(member.getId(), item.getId(), orderCount);
        orderService.cancelOrder(orderId);

        // then
        Order foundOrder = orderRepository.findOne(orderId);
        assertThat(foundOrder.getStatus().equals(OrderStatus.CANCEL));
        assertThat(item.getStockQuantity() == 10);
    }
}