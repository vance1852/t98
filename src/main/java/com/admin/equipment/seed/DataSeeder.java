package com.admin.equipment.seed;

import com.admin.equipment.model.*;
import com.admin.equipment.repo.*;
import com.admin.equipment.security.PasswordUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/** 启动时初始化管理员与种子业务数据（幂等）。 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository userRepo;
    private final EquipmentRepository equipmentRepo;
    private final WorkOrderRepository workOrderRepo;
    private final KnowledgeCaseRepository knowledgeCaseRepo;
    private final KnowledgeCategoryRepository categoryRepo;
    private final KnowledgeTagRepository tagRepo;

    @Value("${app.admin-username}")
    private String adminUsername;

    @Value("${app.admin-password}")
    private String adminPassword;

    public DataSeeder(AppUserRepository userRepo, EquipmentRepository equipmentRepo,
                      WorkOrderRepository workOrderRepo, KnowledgeCaseRepository knowledgeCaseRepo,
                      KnowledgeCategoryRepository categoryRepo, KnowledgeTagRepository tagRepo) {
        this.userRepo = userRepo;
        this.equipmentRepo = equipmentRepo;
        this.workOrderRepo = workOrderRepo;
        this.knowledgeCaseRepo = knowledgeCaseRepo;
        this.categoryRepo = categoryRepo;
        this.tagRepo = tagRepo;
    }

    @Override
    public void run(String... args) {
        if (!userRepo.existsByUsername(adminUsername)) {
            AppUser admin = new AppUser();
            admin.setUsername(adminUsername);
            admin.setPasswordHash(PasswordUtil.hash(adminPassword));
            admin.setDisplayName("平台管理员");
            userRepo.save(admin);
            System.out.println("已创建管理员账号");
        }

        boolean equipmentSeeded = equipmentRepo.count() > 0;
        Equipment e1 = null, e2 = null, e3 = null, e4 = null;

        if (!equipmentSeeded) {
            e1 = newEquip("EQ-1001", "一号注塑机", "注塑车间A区", "robot", "normal");
            e2 = newEquip("EQ-1002", "二号空压机", "动力站", "pump", "warning");
            e3 = newEquip("EQ-1003", "主输送带", "包装车间", "conveyor", "fault");
            e4 = newEquip("EQ-1004", "冷却循环水泵", "动力站", "pump", "maintenance");
            equipmentRepo.saveAll(List.of(e1, e2, e3, e4));

            workOrderRepo.saveAll(List.of(
                    newOrder(e2.getId(), "空压机压力异常巡检", "inspection", "high", "巡检发现排气压力波动，需排查", "王工", "open"),
                    newOrder(e3.getId(), "输送带断带抢修", "repair", "urgent", "包装线输送带断裂，停机抢修", "李工", "in_progress"),
                    newOrder(e4.getId(), "循环水泵季度保养", "maintenance", "medium", "按计划做季度保养换油", "张工", "open"),
                    newOrder(e1.getId(), "注塑机模具点检", "inspection", "low", "例行模具与液压点检", "赵工", "done")
            ));
            System.out.println("基础业务种子数据初始化完成");
        }

        if (knowledgeCaseRepo.count() == 0) {
            seedKnowledgeBase();
            System.out.println("知识库种子数据初始化完成");
        }
    }

    private void seedKnowledgeBase() {
        KnowledgeCategory c1 = newCategory("机械故障", "机械设备相关故障案例", 1);
        KnowledgeCategory c2 = newCategory("电气故障", "电气系统相关故障案例", 2);
        KnowledgeCategory c3 = newCategory("液压故障", "液压系统相关故障案例", 3);
        KnowledgeCategory c4 = newCategory("气动故障", "气动系统相关故障案例", 4);
        categoryRepo.saveAll(List.of(c1, c2, c3, c4));

        KnowledgeTag t1 = newTag("压力异常");
        KnowledgeTag t2 = newTag("温度过高");
        KnowledgeTag t3 = newTag("异响");
        KnowledgeTag t4 = newTag("泄漏");
        KnowledgeTag t5 = newTag("振动");
        KnowledgeTag t6 = newTag("电机故障");
        KnowledgeTag t7 = newTag("轴承损坏");
        KnowledgeTag t8 = newTag("堵塞");
        tagRepo.saveAll(List.of(t1, t2, t3, t4, t5, t6, t7, t8));

        knowledgeCaseRepo.saveAll(List.of(
            newKnowledgeCase(c1.getId(), "pump", "水泵压力不足故障排查与处理",
                "水泵运行时出口压力明显低于额定值，流量减小，系统供水不足",
                "1. 叶轮磨损严重，间隙过大\n2. 入口滤网堵塞，进水不畅\n3. 密封环老化泄漏\n4. 电机转速不足",
                "1. 停机断电，关闭进出口阀门\n2. 拆卸泵壳检查叶轮磨损情况，磨损超差则更换\n3. 清洗入口滤网，清除杂物\n4. 检查更换密封环\n5. 检查电机转速及电源电压\n6. 重新组装后试运行，检测压力",
                "叶轮组件1套, 密封环2个, O型密封圈若干",
                120, "压力不足,叶轮磨损,滤网堵塞", "王工", 15, 8),
            newKnowledgeCase(c1.getId(), "pump", "水泵机械密封泄漏处理",
                "水泵轴封处漏水明显，运行时有水滴甩出",
                "1. 机械密封动静环磨损\n2. 密封弹簧失效\n3. 冷却水不足导致干磨\n4. 轴套磨损",
                "1. 停机排放泵腔介质\n2. 拆卸联轴器及泵盖\n3. 取出机械密封组件检查\n4. 更换动静环及弹簧\n5. 检查轴套磨损情况，必要时更换\n6. 重新组装，调整压缩量\n7. 通水试运行",
                "机械密封组件1套, 轴套1个, 密封脂1盒",
                90, "泄漏,机械密封,轴封", "李工", 22, 12),
            newKnowledgeCase(c1.getId(), "conveyor", "输送带跑偏故障调整",
                "输送带运行时向一侧偏移，摩擦机架边缘，严重时会脱出滚筒",
                "1. 滚筒安装不平行\n2. 托辊组安装不正\n3. 物料落料点偏移\n4. 输送带接头不正\n5. 张紧力不足",
                "1. 停机检查头尾滚筒平行度，调整至公差范围内\n2. 检查调整各托辊组位置，设置调心托辊\n3. 调整落料斗位置，使物料落在输送带中心\n4. 检查输送带接头，歪斜严重需重新接驳\n5. 调整张紧装置，保证足够张紧力\n6. 空载试运行观察跑偏情况",
                "调心托辊2组, 输送带胶接材料",
                60, "跑偏,输送带,滚筒", "张工", 18, 6),
            newKnowledgeCase(c1.getId(), "conveyor", "滚筒轴承异响故障处理",
                "输送带滚筒运行时有明显异响，伴随振动，温度升高",
                "1. 轴承润滑脂不足或变质\n2. 轴承磨损严重，游隙过大\n3. 轴承内圈与轴配合松动\n4. 轴承座安装不正",
                "1. 停机断电，挂牌上锁\n2. 拆卸轴承座端盖，检查润滑脂状态\n3. 油脂变质则清洗轴承，重新加注润滑脂\n4. 检查轴承磨损情况，磨损严重则更换\n5. 检查轴承座安装对中度\n6. 按力矩要求紧固螺栓\n7. 试运行听声音、测温度",
                "轴承1套, 润滑脂1罐, 密封圈2个",
                75, "异响,轴承,振动,温度", "赵工", 25, 10),
            newKnowledgeCase(c2.getId(), "motor", "电机过热跳闸故障处理",
                "电机运行一段时间后过热，热继电器跳闸保护",
                "1. 负载过重，过载运行\n2. 三相电压不平衡\n3. 电机绕组绝缘老化\n4. 散热风扇损坏，通风不良\n5. 轴承损坏导致摩擦增大",
                "1. 断电冷却，测量电机绝缘电阻\n2. 检查三相电源电压是否平衡\n3. 检查负载情况，核实是否过载\n4. 检查电机散热风扇及风罩\n5. 测量三相绕组直流电阻，判断是否匝间短路\n6. 检查轴承状态，听声音测温度\n7. 清理电机表面积灰，改善通风条件",
                "电机轴承1套, 润滑脂, 风扇叶1个",
                90, "过热,跳闸,电机,过载", "王工", 30, 15),
            newKnowledgeCase(c2.getId(), "motor", "电机启动困难故障诊断",
                "电机启动时嗡嗡响，无法正常启动，电流很大",
                "1. 电源缺相\n2. 定子绕组断路或短路\n3. 负载卡滞或过重\n4. 启动电容损坏（单相电机）\n5. 轴承抱死",
                "1. 检查三相电源电压及接触器触点\n2. 测量电机三相绕组直流电阻，判断绕组是否正常\n3. 盘车检查机械负载是否灵活\n4. 检查轴承是否转动灵活\n5. 单相电机检查启动电容容量\n6. 确认控制回路接线正确",
                "启动电容1个, 接触器触点1副",
                45, "启动困难,缺相,绕组", "李工", 12, 5),
            newKnowledgeCase(c3.getId(), "robot", "注塑机液压系统压力不稳定",
                "液压系统压力波动大，压力表指针剧烈摆动，执行元件动作不稳",
                "1. 液压油混入空气\n2. 油泵磨损严重\n3. 溢流阀故障\n4. 吸油过滤器堵塞\n5. 液压油粘度不合适",
                "1. 检查油箱油位及油液状态\n2. 对系统进行排气操作\n3. 检查清洗吸油过滤器\n4. 检查油泵工作状态，听声音测压力\n5. 检查溢流阀阀芯是否卡滞，清洗或更换\n6. 检测液压油粘度，必要时更换\n7. 检查各管路接头是否漏气",
                "液压油, 滤芯1个, 溢流阀1个",
                150, "液压,压力不稳,油泵,溢流阀", "张工", 20, 9),
            newKnowledgeCase(c3.getId(), "robot", "注塑机开模无力故障排查",
                "开模时动作缓慢无力，甚至无法开模，系统压力显示正常",
                "1. 开模阀阀芯卡滞\n2. 开模液压缸内泄\n3. 模具未充分冷却，粘模\n4. 顶针未退回到位\n5. 锁模力过大",
                "1. 检查开模电磁阀是否得电，阀芯是否动作\n2. 清洗或更换开模阀\n3. 检查液压缸内泄情况，必要时更换密封\n4. 检查顶针是否退回到位\n5. 适当降低锁模力\n6. 检查模具冷却系统，确保充分冷却",
                "电磁阀1个, 液压缸密封套件1套",
                180, "开模无力,液压,注塑机,电磁阀", "赵工", 16, 7),
            newKnowledgeCase(c4.getId(), "pump", "空压机排气温度过高",
                "空压机运行中排气温度超过报警值，自动停机保护",
                "1. 冷却器散热不良\n2. 润滑油不足或变质\n3. 空气过滤器堵塞\n4. 温控阀故障\n5. 环境温度过高",
                "1. 停机冷却后检查油位及油质\n2. 清洗冷却器散热片，去除积灰油污\n3. 检查更换空气过滤器滤芯\n4. 检查温控阀是否正常工作\n5. 检查环境通风情况，改善散热条件\n6. 检查油过滤器是否堵塞\n7. 重新开机观察温度变化",
                "空气滤芯1个, 专用润滑油, 温控阀1个",
                60, "温度高,空压机,冷却,滤芯", "王工", 28, 14),
            newKnowledgeCase(c4.getId(), "pump", "空压机产气不足故障处理",
                "空压机排气量不足，系统压力上不去，供气不足",
                "1. 空气过滤器堵塞\n2. 进气阀开度不足\n3. 卸荷阀故障\n4. 油气分离器堵塞\n5. 系统泄漏严重\n6. 皮带打滑（皮带机）",
                "1. 检查更换空气过滤器滤芯\n2. 检查进气阀动作是否灵活，开度是否足够\n3. 检查卸荷阀工作状态\n4. 检查油气分离器压差，超标则更换\n5. 用肥皂水检查系统管路泄漏点\n6. 检查皮带张紧力及磨损情况\n7. 检查最小压力阀是否正常",
                "空气滤芯, 油气分离器芯, 皮带1条",
                90, "供气不足,空压机,滤芯,泄漏", "李工", 14, 6)
        ));
    }

    private KnowledgeCategory newCategory(String name, String description, int sortOrder) {
        KnowledgeCategory c = new KnowledgeCategory();
        c.setName(name);
        c.setDescription(description);
        c.setSortOrder(sortOrder);
        return c;
    }

    private KnowledgeTag newTag(String name) {
        KnowledgeTag t = new KnowledgeTag();
        t.setName(name);
        t.setUseCount(5);
        return t;
    }

    private KnowledgeCase newKnowledgeCase(Long categoryId, String equipmentType, String title,
                                           String faultSymptom, String causeAnalysis,
                                           String solutionSteps, String spareParts,
                                           int estimatedMinutes, String keywords,
                                           String author, int adoptions, int likes) {
        KnowledgeCase k = new KnowledgeCase();
        k.setCategoryId(categoryId);
        k.setEquipmentType(equipmentType);
        k.setTitle(title);
        k.setFaultSymptom(faultSymptom);
        k.setCauseAnalysis(causeAnalysis);
        k.setSolutionSteps(solutionSteps);
        k.setSpareParts(spareParts);
        k.setEstimatedMinutes(estimatedMinutes);
        k.setTags(keywords);
        k.setKeywords(keywords);
        k.setAuthorUsername(author);
        k.setStatus("published");
        k.setAdoptionCount(adoptions);
        k.setLikeCount(likes);
        k.setViewCount(adoptions * 3 + likes * 2);
        k.setWeightScore(1.0 + adoptions * 0.05 + likes * 0.02);
        k.setPublishedAt(LocalDateTime.now().minusDays(adoptions));
        return k;
    }

    private Equipment newEquip(String code, String name, String location, String type, String status) {
        Equipment e = new Equipment();
        e.setCode(code);
        e.setName(name);
        e.setLocation(location);
        e.setType(type);
        e.setStatus(status);
        return e;
    }

    private WorkOrder newOrder(Long equipmentId, String title, String type, String priority,
                               String description, String assignee, String status) {
        WorkOrder w = new WorkOrder();
        w.setEquipmentId(equipmentId);
        w.setTitle(title);
        w.setType(type);
        w.setPriority(priority);
        w.setDescription(description);
        w.setAssignee(assignee);
        w.setStatus(status);
        return w;
    }
}
