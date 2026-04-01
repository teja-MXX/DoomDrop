package com.doomscroll.destroyer.utils

object RoastContent {

    val lockoutRoasts = listOf(
        "babe you just watched\nsomeone's vacation reel\nfor the 4th time.\nthis is the intervention.",
        "one whole minute.\nthat's all it took.\nnow go drink water\nand think about your choices.",
        "the algorithm fed you\nand you ATE.\nnow the kitchen is\nclosed for 30 mins.",
        "your attention span\njust checked out.\nwe gave it\na 30-min spa break.",
        "imagine explaining to\nyour future self\nthat you needed an app\nto stop looking at brunch pics.",
        "congratulations!\nyou wasted 1 minute\nof your finite life\non strangers' reels. iconic.",
        "the scroll stops here.\ngo drink water.\nyou're mostly dehydrated\nand also tired of you.",
        "instagram is temporarily\nunavailable in your area.\nthe area being:\nyour phone.",
        "your thumbs needed a break.\nyour brain agreed.\nyou weren't consulted.\nthat's fine.",
        "sir/ma'am this is\na productivity app\nand you burned through\n1 min. legendary. locked.",
        "you opened instagram.\ninstagram said 'lol no'.\nwe said 'we got you'.\nyou lose.",
        "every second you spent\non that explore page\nis a second your enemies\nwere out here thriving.",
        "the reel was not\nworth it. none of\nthem are. 30 minutes.\nbye.",
        "you had 1 minute.\nyou used it.\nnow you have 0 minutes.\nmath is wild.",
        "whatever you were\njust looking at:\nit wasn't important.\nthis lockout is."
    )

    val dailyExhaustRoasts = listOf(
        "20 minutes.\nall of them.\ngone.\ninstagram doesn't exist\nfor you today.",
        "you burned through\nyour entire daily budget.\nwe're not mad.\nwe're just disappointed.",
        "today's instagram\nallocation: DEPLETED.\ntomorrow's instagram\nallocation: maybe use it wisely?",
        "the doomscroll\nends here.\nyou had your ration.\ncome back at midnight.",
        "20 whole minutes\nof your one life.\nspent on reels.\nwe hope it was worth it.",
        "your daily quota\nhas left the chat.\ninstagram has left the chat.\ngo outside.",
    )

    // Shown briefly just before locking (the 10-second warning)
    val warningMessages = listOf(
        "⚠ 10 seconds left\nbefore lockout...",
        "closing time\nin 10 seconds 👋",
        "the bouncer is\ncoming. 10 sec.",
        "tick tock tick tock\n10 more seconds...",
        "say goodbye\nto the feed. 10s.",
    )

    // Doodle emoji faces for the overlay
    val doodleFaces = listOf(
        "😤", "🫠", "😑", "🙄", "😮‍💨",
        "🤦", "💀", "🫡", "😐", "🤡",
        "😒", "🥲", "😬", "🤌", "👋"
    )

    fun randomLockoutRoast() = lockoutRoasts.random()
    fun randomDailyRoast() = dailyExhaustRoasts.random()
    fun randomWarning() = warningMessages.random()
    fun randomFace() = doodleFaces.random()
}
