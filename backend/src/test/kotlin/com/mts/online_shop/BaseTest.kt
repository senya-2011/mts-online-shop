package com.mts.online_shop

import com.mts.online_shop.config.TestConfig
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@Import(TestConfig::class)
@ActiveProfiles("test")
abstract class BaseTest
