# 목차
🎀 [순수 JPA Repository와 QueryDSL](#순수-jpa-repository와-querydsl)
🎀 [동적 쿼리와 성능 최적화 조회](#동적-쿼리와-성능-최적화-조회)
🎀 [조회 API 컨트롤러 개발](#조회-api-컨트롤러-개발)

## 순수 JPA Repository와 QueryDSL
### [순수 JPA Repository - MemberJPARepository](..%2Fsrc%2Fmain%2Fjava%2Fstudy%2Fquerydsl%2Frepository%2FMemberJPARepository.java)
```java
@Repository
public class MemberJPARepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberJPARepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }
}
```

### [순수 JPA 레포지토리 테스트 - MemberJPARepositoryTest](..%2Fsrc%2Ftest%2Fjava%2Fstudy%2Fquerydsl%2Frepository%2FMemberJPARepositoryTest.java)
```java
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class MemberJPARepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJPARepository memberJpaRepository;


    @Test
    void basicTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);
        Member findMember = memberJpaRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);
        List<Member> result1 = memberJpaRepository.findAll();
        assertThat(result1).containsExactly(member);
        List<Member> result2 = memberJpaRepository.findByUsername("member1");
        assertThat(result2).containsExactly(member);
    }
}
```

### QueryDSL 사용
#### [순수 JPA Repository에 QueryDSL 추가](..%2Fsrc%2Fmain%2Fjava%2Fstudy%2Fquerydsl%2Frepository%2FMemberJPARepository.java)
```java
public List<Member> findAll_QueryDSL() {
    return queryFactory
            .selectFrom(member).fetch();
}

public List<Member> findByUsername_QueryDSL(String username) {
    return queryFactory
            .selectFrom(member)
            .where(member.username.eq(username))
            .fetch();
}
```

#### QueryDSL 테스트 추가
```java
@Test
void basicQueryDSLTest() {
    Member member = new Member("member1", 10);
    memberJpaRepository.save(member);

    Member findMember = memberJpaRepository.findById(member.getId()).get();
    assertThat(findMember).isEqualTo(member);

    List<Member> result1 = memberJpaRepository.findAll_QueryDSL();
    assertThat(result1).containsExactly(member);

    List<Member> result2 = memberJpaRepository.findByUsername_QueryDSL("member1");
    assertThat(result2).containsExactly(member);
} 
```

### [JPAQueryFactory 스프링 빈 등록 - QuerydslApplication](..%2Fsrc%2Fmain%2Fjava%2Fstudy%2Fquerydsl%2FQuerydslApplication.java)
다음과 같이 `JPAQueryFactory`를 스프링 빈으로 등록해서 주입받아 사용해도 된다.

```java
@Bean
JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
	return new JPAQueryFactory(entityManager);
}
```

```java
@Repository
@RequiredArgsConstructor
public class MemberJPARepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;
    
    ...
}
```

> 🍀 동시성 문제는 걱정하지 않아도 된다. 왜냐하면 여기서 스프링이 주입해주는 엔티티 매니저는 실제 동작 시점에 진짜 엔티티 매니저를 찾아주는 프록시용 가짜 엔티티 매니저이다.
> 이 가짜 엔티티 매니저는 실제 사용 시점에 트랜잭션 단위로 실제 엔티티 매니저(영속성 컨텍스트)를 할당해준다.

## 동적 쿼리와 성능 최적화 조회
### [조회 최적화용 DTO 추가 - MemberTeamDTO](..%2Fsrc%2Fmain%2Fjava%2Fstudy%2Fquerydsl%2Fdto%2FMemberTeamDTO.java)
```java
@Data
public class MemberTeamDTO {
    private Long memberId;
    private String username;
    private int age;
    private Long teamId;
    private String teamName;

    @QueryProjection
    public MemberTeamDTO(Long memberId, String username, int age, Long teamId, String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}
```

- `@QueryProjection`을 추가했다. `QMemberTeamDTO`를 생성하기 위해 `compile` 파일을 실행해야 한다.

### [회원 검색 조건 - MemberSearchCondition](..%2Fsrc%2Fmain%2Fjava%2Fstudy%2Fquerydsl%2Fdto%2FMemberSearchCondition.java)
```java
@Data
public class MemberSearchCondition {
    
    private String username;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;
}
```

### [`Builder` 사용 - MemberJPARepository](..%2Fsrc%2Fmain%2Fjava%2Fstudy%2Fquerydsl%2Frepository%2FMemberJPARepository.java)
```java
public List<MemberTeamDTO> searchByBuilder(MemberSearchCondition condition) {

    BooleanBuilder builder = new BooleanBuilder();

    if (hasText(condition.getUsername()))
        builder.and(member.username.eq(condition.getUsername()));

    if (hasText(condition.getTeamName()))
        builder.and(team.name.eq(condition.getTeamName()));

    if (condition.getAgeGoe() != null)
        builder.and(member.age.goe(condition.getAgeGoe()));

    if (condition.getAgeLoe() != null)
        builder.and(member.age.loe(condition.getAgeLoe()));

    return queryFactory
            .select(new QMemberTeamDTO(
                    member.id,
                    member.username,
                    member.age,
                    team.id,
                    team.name
            ))
            .from(member)
            .leftJoin(member.team, team)
            .where(builder)
            .fetch();
}
```

#### [조회 예제 테스트 - MemberJPARepositoryTest](..%2Fsrc%2Ftest%2Fjava%2Fstudy%2Fquerydsl%2Frepository%2FMemberJPARepositoryTest.java)
```java
@Test
void 조회_예제_테스트() {
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

    List<MemberTeamDTO> result = memberJpaRepository.searchByBuilder(condition);

    assertThat(result).extracting("username").containsExactly("member4");
}
```

### [Where절 파라미터 사용 - MemberJPARepository](..%2Fsrc%2Fmain%2Fjava%2Fstudy%2Fquerydsl%2Frepository%2FMemberJPARepository.java)
```java
public List<MemberTeamDTO> search(MemberSearchCondition condition) {
    return queryFactory
            .select(new QMemberTeamDTO(
                    member.id,
                    member.username,
                    member.age,
                    team.id,
                    team.name
            ))
            .from(member)
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
```

> 🍀 Where 절에 파라미터 방식을 사용하면 조건을 재사용할 수 있다.
> ```java
> public List<Member> findMember(MemberSearchCondition condition) {
>     return queryFactory
>            .selectFrom(member)
>            .leftJoin(member.team, team)
>            .where(usernameEq(condition.getUsername()),
>                    teamNameEq(condition.getTeamName()),
>                    ageGoe(condition.getAgeGoe()),
>                    ageLoe(condition.getAgeLoe()))
>            .fetch();
> }
> ```

## 조회 API 컨트롤러 개발