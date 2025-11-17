package io.github.rothes.esu.bukkit.module.essencialcommands

import io.github.rothes.esu.bukkit.module.EssentialCommandsModule
import io.github.rothes.esu.core.module.CommonFeature

abstract class BaseCommand<C, L> : CommonFeature<C, L>() {

    override val name: String = javaClass.simpleName.removeSuffix("Command")
    override val module: EssentialCommandsModule
        get() = super.module as EssentialCommandsModule

}