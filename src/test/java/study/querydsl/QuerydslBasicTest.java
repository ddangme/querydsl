package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        // Given
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        em.flush();
        em.clear();

        // When
        List<Member> members = em.createQuery("SELECT m FROM Member m", Member.class)
                .getResultList();

        // Then
        for (Member member : members) {
            System.out.print("member = " + member);
            System.out.println("-> member.team=" + member.getTeam());
        }
    }

    @DisplayName("JPQL - member1을 찾아라.")
    @Test
    public void startJPQL() {
        Member findMember = em.createQuery("SELECT m FROM Member m WHERE m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @DisplayName("member1을 찾아라")
    @Test
    public void startQuerydsl() {
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) // 파라미터 바인딩 처리
                .fetchOne();

        assertThat(Objects.requireNonNull(findMember).getUsername()).isEqualTo("member1");
    }

    @DisplayName("기본 검색 쿼리")
    @Test
    void search() {
        // Given
        Member findMember = queryFactory
                .selectFrom(member) // select와 from을 합칠 수 있다. 즉, .select(member).form(member)와 같은 코드
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        // Then
        assertThat(Objects.requireNonNull(findMember).getUsername()).isEqualTo("member1");
        assertThat(Objects.requireNonNull(findMember).getAge()).isEqualTo(10);
    }

    void searchCode() {
        member.username.eq("member1"); // username = "member1"
        member.username.ne("member1"); // username != "member1"
        member.username.eq("member1").not(); // username != "member1"

        member.username.isNotNull(); // 이름이 is not null

        member.age.in(10, 20); // age in (10, 20)
        member.age.notIn(10, 20); // age not in (10, 20)
        member.age.between(10, 30); // between 10, 30

        member.age.goe(30); // age >= 30
        member.age.gt(30); // age > 30
        member.age.loe(30); // age <= 30
        member.age.lt(30); // age < 30

        member.username.like("member%"); // like 검색
        member.username.contains("member"); // like "%member%" 검색
        member.username.startsWith("member"); //like "member%" 검색
    }


}
