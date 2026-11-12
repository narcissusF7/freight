package com.lukachangtong.freight;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 单体模块边界守护：按二级包切片，禁止模块间循环依赖。
 * 后续新增业务模块时若出现「大泥球」，这条规则会在 CI 直接失败。
 */
@AnalyzeClasses(packages = "com.lukachangtong.freight")
class ModuleBoundaryTest {

    @ArchTest
    static final ArchRule no_cycles_between_modules =
            slices().matching("com.lukachangtong.freight.(*)..")
                    .should().beFreeOfCycles();
}
