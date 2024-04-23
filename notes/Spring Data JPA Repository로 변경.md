# 목차
🎀 [Spring Data JPA Repository로 변경](#spring-data-jpa-repository로-변경)  
🎀 [사용자 정의 Repository](#사용자-정의-repository)  
🎀 [Spring Data 페이징 활용 1 - QueryDSL 페이징 연동](#spring-data-페이징-활용-1---querydsl-페이징-연동)  
🎀 [Spring Data 페이징 활용 2 - CountQuery 최적화](#spring-data-페이징-활용-2---countquery-최적화)  
🎀 [Spring Data 페이징 활용 3 - 컨트롤러 개발](#spring-data-페이징-활용-3---컨트롤러-개발)  

## Spring Data JPA Repository로 변경
### [Spring Data JPA Repository 생성](..%2Fsrc%2Fmain%2Fjava%2Fstudy%2Fquerydsl%2Frepository%2FMemberRepository.java)
```java
public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByUsername(String username);
}
```

### [Spring Data JPA Repository 테스트 생성](..%2Fsrc%2Ftest%2Fjava%2Fstudy%2Fquerydsl%2Frepository%2FMemberRepositoryTest.java)
```java
@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Test
    void basicTest() {
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberRepository.findAll();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberRepository.findByUsername("member1");
        assertThat(result2).containsExactly(member);
    }

}
```

`QueryDSL` 전용 기능인 회원 search를 작성할 수 없다. ➡️ **사용자 정의 Repository가 필요하다.**

## 사용자 정의 Repository
### 사용자 정의 Repository 사용법
1. [사용자 정의 인터페이스 작성](#사용자-정의-인터페이스-작성)
2. [사용자 정의 인터페이스 구현](#사용자-정의-인터페이스-구현)
3. [Spring Data Repository에 사용자 정의 인터페이스 상속](#spring-data-repository에-사용자-정의-인터페이스-상속)

![사용자 정의 리포지토리 구성.png](imgs%2F%EC%82%AC%EC%9A%A9%EC%9E%90%20%EC%A0%95%EC%9D%98%20%EB%A6%AC%ED%8F%AC%EC%A7%80%ED%86%A0%EB%A6%AC%20%EA%B5%AC%EC%84%B1.png)

#### [사용자 정의 인터페이스 작성](..%2Fsrc%2Fmain%2Fjava%2Fstudy%2Fquerydsl%2Frepository%2FMemberRepositoryCustom.java)
```java
public interface MemberRepositoryCustom {

    List<MemberTeamDTO> search(MemberSearchCondition condition);
}
```

#### [사용자 정의 인터페이스 구현](..%2Fsrc%2Fmain%2Fjava%2Fstudy%2Fquerydsl%2Frepository%2FMemberRepositoryImpl.java)
```java
package study.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDTO;
import study.querydsl.dto.QMemberTeamDTO;

import javax.persistence.EntityManager;
import java.util.List;

import static org.springframework.util.ObjectUtils.isEmpty;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<MemberTeamDTO> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDTO(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name
                )).from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .fetch();
    }

    private BooleanExpression usernameEq(String username) {
        return isEmpty(username) ? null : member.username.eq(username);
    }

    private BooleanExpression teamNameEq(String teamName) {
        return isEmpty(teamName) ? null : team.name.eq(teamName);
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe == null ? null : member.age.goe(ageGoe);
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe == null ? null : member.age.loe(ageLoe);
    }
}
```

#### [Spring Data Repository에 사용자 정의 인터페이스 상속]
```java
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom { ... }
```

### 사용자 정의 Repository 테스트
```java
@Test
void searchTest() {
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

    MemberSearchCondition condition = new MemberSearchCondition();
    condition.setAgeGoe(35);
    condition.setAgeLoe(40);
    condition.setTeamName("teamB");

    List<MemberTeamDTO> result = memberRepository.search(condition);

    assertThat(result).extracting("username").containsExactly("member4");
}
```

## Spring Data 페이징 활용 1 - QueryDSL 페이징 연동
- 스프링 데이터의 `Page`, `Pageable`을 활용하기.
- [전체 카운트를 한 번에 조회하는 단순한 방법](#전체-카운트를-한-번에-조회하는-단순한-방법)
- [데이터 내용과 전체 카운트를 별도로 조회하는 방법](#데이터-내용과-전체-카운트를-별도로-조회하는-방법)

### [사용자 정의 인터페이스에 페이징 2가지 추가](..%2Fsrc%2Fmain%2Fjava%2Fstudy%2Fquerydsl%2Frepository%2FMemberRepositoryCustom.java)
```java
public interface MemberRepositoryCustom {

    List<MemberTeamDTO> search(MemberSearchCondition condition);

    Page<MemberTeamDTO> searchPageSimple(MemberSearchCondition condition, Pageable pageable);

    Page<MemberTeamDTO> searchPageComplex(MemberSearchCondition condition, Pageable pageable);
}
```

### [전체 카운트를 한 번에 조회하는 단순한 방법](..%2Fsrc%2Fmain%2Fjava%2Fstudy%2Fquerydsl%2Frepository%2FMemberRepositoryImpl.java)
```java
/**
 * 단순한 페이징, fetchResults() 사용
 */
@Override
public Page<MemberTeamDTO> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
    QueryResults<MemberTeamDTO> results = queryFactory
            .select(new QMemberTeamDTO(
                    member.id,
                    member.username,
                    member.age,
                    team.id,
                    team.name))
            .from(member)
            .leftJoin(member.team, team)
            .where(usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetchResults();

    List<MemberTeamDTO> content = results.getResults();

    long total = results.getTotal();
    return new PageImpl<>(content, pageable, total);
}
```
- Querydsl이 제공하는 `fetchResults()` 를 사용하면 내용과 전체 카운트를 한번에 조회할 수 있다.(실제 쿼리 는 2번 호출)
- `fetchResult()` 는 카운트 쿼리 실행시 필요없는 `order by` 는 제거한다.

### [데이터 내용과 전체 카운트를 별도로 조회하는 방법](..%2Fsrc%2Fmain%2Fjava%2Fstudy%2Fquerydsl%2Frepository%2FMemberRepositoryImpl.java)
```java
/**
 * 복잡한 페이징
 * 데이터 조회 쿼리와 전체 카운트 쿼리를 분리
 */
@Override
public Page<MemberTeamDTO> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
    List<MemberTeamDTO> content = queryFactory
            .select(new QMemberTeamDTO(
                    member.id,
                    member.username,
                    member.age,
                    team.id,
                    team.name
            )).from(member)
            .leftJoin(member.team, team)
            .where(usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    long total = queryFactory
            .select(member)
            .from(member)
            .leftJoin(member.team, team)
            .where(usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe()))
            .fetchCount();
    
    return new PageImpl<>(content, pageable, total);
}
```
- 전체 카운트를 조회하는 방법을 최적화할 수 있으면 이렇게 분리하면 된다. (예를 들어서 전체 카운트를 조회할 때 조인 쿼리를 줄일 수 있다면 상당한 효과가 있다.)
- 코드를 리팩토링해서 내용 쿼리와 전체 카운트 쿼리를 읽기 좋게 분리하면 좋다.

## Spring Data 페이징 활용 2 - CountQuery 최적화
### PageableExecutionUtils.getPage()로 최적화
```java
@Override
public Page<MemberTeamDTO> searchPageCountQuery(MemberSearchCondition condition, Pageable pageable) {
    List<MemberTeamDTO> content = queryFactory
            .select(new QMemberTeamDTO(
                    member.id,
                    member.username,
                    member.age,
                    team.id,
                    team.name
            )).from(member)
            .leftJoin(member.team, team)
            .where(usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
    
    JPAQuery<Member> countQuery = queryFactory
            .select(member)
            .from(member)
            .leftJoin(member.team, team)
            .where(usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe()));

    return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
}
```
- 스프링 데이터 라이브러리가 제공 
- count 쿼리가 생략 가능한 경우 생략해서 처리 
  - 페이지 시작이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때 
  - 마지막 페이지 일 때 (offset + 컨텐츠 사이즈를 더해서 전체 사이즈 구함, 더 정확히는 마지막 페이지이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때)

## [Spring Data 페이징 활용 3 - 컨트롤러 개발](..%2Fsrc%2Fmain%2Fjava%2Fstudy%2Fquerydsl%2Fcontroller%2FMemberController.java)
```java
@GetMapping("/v2/members")
public Page<MemberTeamDTO> searchMemberV2(MemberSearchCondition condition, Pageable pageable) {
    return memberRepository.searchPageSimple(condition, pageable);
}

@GetMapping("/v3/members")
public Page<MemberTeamDTO> searchMemberV3(MemberSearchCondition condition, Pageable pageable) {
    return memberRepository.searchPageCountQuery(condition, pageable);
}
```

### 실행 결과
`http://localhost:8080/v2/members?size=5&page=2` or `http://localhost:8080/v3/members?size=5&page=2` 
```text
   {
    "content": [
        {
            "memberId": 11,
            "username": "member10",
            "age": 10,
            "teamId": 1,
            "teamName": "teamA"
        },
        {
            "memberId": 12,
            "username": "member11",
            "age": 11,
            "teamId": 2,
            "teamName": "teamB"
        },
        {
            "memberId": 13,
            "username": "member12",
            "age": 12,
            "teamId": 1,
            "teamName": "teamA"
        },
        {
            "memberId": 14,
            "username": "member13",
            "age": 13,
            "teamId": 2,
            "teamName": "teamB"
        },
        {
            "memberId": 15,
            "username": "member14",
            "age": 14,
            "teamId": 1,
            "teamName": "teamA"
        }
    ],
    "pageable": {
        "sort": {
            "empty": true,
            "sorted": false,
            "unsorted": true
        },
        "offset": 10,
        "pageNumber": 2,
        "pageSize": 5,
        "paged": true,
        "unpaged": false
    },
    "last": false,
    "totalPages": 20,
    "totalElements": 100,
    "first": false,
    "size": 5,
    "number": 2,
    "sort": {
        "empty": true,
        "sorted": false,
        "unsorted": true
    },
    "numberOfElements": 5,
    "empty": false
}
```

### 스프링 데이터 정렬 (Sort)
스프링 데이터 JPA는 자신의 정렬(Sort)를 QueryDSL의 정렬(OrderSpecifier)로 편리하게 변경하는 기능을 제공한다.
이 부분은 뒤에 스프링 데이터 JPA가 제공하는 QueryDSL 기능에서 알아본다.

스프링 데이터의 정렬을 QueryDSL의 정렬로 직접 전환하는 방법은 다음 코드를 참고하자.
#### 스프링 데이터 Sort를 QueryDSL의 OrderSpecifier로 변환
```java
JPAQuery<Member> query = queryFactory
        .selectFrom(member);

for (Sort.Order o : pageable.getSort()) {
    PathBuilder pathBuilder = new PathBuilder(member.getType(), member.getMetadata());
    query.orderBy(new OrderSpecifier(o.isAscending() ? Order.ASC : Order.DESC, pathBuilder.get(o.getProperty())));
}

List<Member> result = query.fetch();
```

> 🍀 정렬(`Sort`)은 조건이 조금만 복잡해져도 `Pageable`의 `Sort` 기능을 사용하기 어렵다.
> 루트 엔티티 범위를 넘어가는 동적 정렬 기능이 필요하면 스프링 데이터 페이징이 제공하는 `Sort`를 사용하기 보다는 파라미터를 직접 받아서 처리하는 것을 권장한다.