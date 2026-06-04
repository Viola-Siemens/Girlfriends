# Story 10 测试与回归验收 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐底层系统自动化测试、构建验证和单人/多人手工验收清单，确保 v0.1.0 底层系统可进入内容接入阶段。

**Architecture:** 自动化测试覆盖纯规则、持久化和网络权限边界；Minecraft 内实际 UI、实体、结构与事件表现用 GameTest 或手工验收记录补足。未有 GDD 的内容不作为完成条件。

**Tech Stack:** Gradle、JUnit Platform、NeoForge GameTest、Minecraft 客户端/服务端手工验收。

---

## Files

- Modify: `docs/v0.1.0/PLAN_system.md`
- Create: `docs/v0.1.0/TEST_system.md`
- Modify or create: relevant test classes from Story 1 to Story 9

## Steps

- [ ] **Step 1: Create regression checklist document**

Create `docs/v0.1.0/TEST_system.md`:

```markdown
# Girlfriends v0.1.0 底层系统测试与回归记录

## 自动化测试

| 模块 | 测试类 | 覆盖点 | 结果 |
| --- | --- | --- | --- |
| 持久化 | `GirlfriendsWorldDataTest` | 关系状态序列化与反序列化 | 待执行 |
| 好感度 | `RelationshipServiceTest` | 裁剪、阶段、每日重置 | 待执行 |
| 礼物 | `GiftServiceTest` | 档位、公式、每日上限、拒收 | 待执行 |
| 委托 | `QuestServiceTest` | 单槽、固定委托、随机过期 | 待执行 |
| 绑定 | `BindingServiceTest` | 爱慕绑定、动摇期、亲密锁定 | 待执行 |
| 家园 | `HomeServiceTest` | 入住、收益、争执 | 待执行 |
| 祝福 | `BlessingServiceTest` | 启用条件、数值修正、概率 | 待执行 |
| 死亡重生 | `CharacterRespawnServiceTest` | 死亡清零、奖励保留、庇护所 | 待执行 |
| 网络摘要 | `InteractionSummaryServiceTest` | 模糊好感、按钮权限摘要 | 待执行 |
| 网络权限 | `ServerboundPermissionTest` | 越权请求不改变状态 | 待执行 |

## 手工验收

| 场景 | 步骤 | 预期 | 结果 |
| --- | --- | --- | --- |
| 单人关系 | 创建世界，与同一角色进行好感变化操作 | 仅该玩家该角色关系变化 | 待执行 |
| 多人竞争 | 两名玩家提升同一角色好感并触发动摇期 | 3 个游戏日后领先者获得绑定 | 待执行 |
| 亲密锁定 | 一名玩家完成亲密确认 | 其他玩家无法继续赠礼和接取委托 | 待执行 |
| 家园绑定 | 玩家完成入住条件后绑定双人床 | 同一玩家只能有一名家园伙伴 | 待执行 |
| 角色死亡 | 击杀角色本体 | 关系、委托、绑定、家园清零，终章奖励保留 | 待执行 |
| 网络越权 | 客户端构造无权限赠礼或接取请求 | 服务端拒绝且 SavedData 不变 | 待执行 |

## 构建验证

- `./gradlew test`：待执行
- `./gradlew build`：待执行
```

- [ ] **Step 2: Run full tests**

Run:

```bash
./gradlew test
```

Expected: all unit tests pass. Update `TEST_system.md` 自动化测试结果 from `待执行` to `通过` for each passing test class.

- [ ] **Step 3: Run build**

Run:

```bash
./gradlew build
```

Expected: build succeeds. Update build verification section with `通过`.

- [ ] **Step 4: Start client for smoke test**

Run:

```bash
./gradlew runClient
```

Expected: Minecraft client starts with mod loaded. If client cannot start due environment limitations, record blocker in `TEST_system.md` instead of marking pass.

- [ ] **Step 5: Start server or GameTest smoke test**

Run one of:

```bash
./gradlew runServer
```

or

```bash
./gradlew runGameTestServer
```

Expected: server starts with mod loaded. Record result in `TEST_system.md`.

- [ ] **Step 6: Verify no GDD-dependent scope slipped in**

Check implemented code does not include final role AI, concrete fixed quest narrative objectives, structure decoration evolution, dialogue scripts, or final reward items.

Expected: only framework, interfaces, services and generic rules exist.

- [ ] **Step 7: Update main plan completion section**

Modify `docs/v0.1.0/PLAN_system.md` completion section with actual test command outputs and any manual blockers.

- [ ] **Step 8: Final verification commands**

Run:

```bash
./gradlew test
./gradlew build
```

Expected: both commands pass before claiming implementation complete.

- [ ] **Step 9: Commit checkpoint if requested**

```bash
git add docs/v0.1.0/TEST_system.md docs/v0.1.0/PLAN_system.md src/test/java src/main/java
git commit -m "test(v0.1.0): add system regression coverage"
```
