package study.querydsl;

import com.querydsl.core.QueryResults;
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

    @DisplayName("AND 조건을 파라미터로 처리")
    @Test
    public void searchAndParam() {
        List<Member> findMembers = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"), // 쉼표(,)를 사용할 경우 and 조건과 동일하다.
                        member.age.eq(10)
                )
                .fetch();

        assertThat(findMembers.size()).isEqualTo(1);
    }

    @DisplayName("결과 조회")
    @Test
    void resultFetch() {
        // List 조회
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        // 단건 조회
        Member findMember1 = queryFactory
                .selectFrom(member)
                .fetchOne();

        // 검색 결과 처음의 한 건 조회
        Member findMember2 = queryFactory
                .selectFrom(member)
                .fetchFirst();

        // 페이징에서 사용 (QueryDSL 5.0 부터 사용하지 않는다.)
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        // count 쿼리로 변경 (QueryDSL 5.0 부터 사용하지 않는다.)
        long count = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순 (desc)
     * 2. 회원 이름 오름차순 (asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력 (nulls last)
     */
    @DisplayName("정렬")
    @Test
    void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(),
                        member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }


}
