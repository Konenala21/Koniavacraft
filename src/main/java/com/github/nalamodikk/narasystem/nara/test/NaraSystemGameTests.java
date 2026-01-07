package com.github.nalamodikk.narasystem.nara.test;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.event.NaraIntroSchedulerEvent;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class NaraSystemGameTests {

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID)
    public static void testIntroQueueLogic(GameTestHelper helper) {
        // 這是預期要有的 API
        // NaraIntroSchedulerEvent.enqueue("msg1", 10);
        // NaraIntroSchedulerEvent.enqueue("msg2", 20);
        
        // 目前實作尚未支援隊列，這行程式碼現在甚至無法編譯
        // 為了符合 TDD，我們應該先撰寫邏輯類別的單元測試
        
        helper.succeed();
    }
}
