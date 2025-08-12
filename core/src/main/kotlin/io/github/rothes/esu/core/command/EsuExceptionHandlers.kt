package io.github.rothes.esu.core.command

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.incendo.cloud.caption.Caption
import org.incendo.cloud.caption.StandardCaptionKeys
import org.incendo.cloud.exception.ArgumentParseException
import org.incendo.cloud.exception.CommandExecutionException
import org.incendo.cloud.exception.InvalidCommandSenderException
import org.incendo.cloud.exception.InvalidSyntaxException
import org.incendo.cloud.exception.NoPermissionException
import org.incendo.cloud.exception.NoSuchCommandException
import org.incendo.cloud.exception.handling.ExceptionContext
import org.incendo.cloud.exception.handling.ExceptionController
import org.incendo.cloud.util.TypeUtils

class EsuExceptionHandlers(
    private val exceptionController: ExceptionController<out User>,
) {

    fun register() {
        with(exceptionController) {
            registerHandler(Throwable::class.java) { context ->
                context.msg(StandardCaptionKeys.EXCEPTION_UNEXPECTED)
                EsuCore.instance.err("An unhandled exception was thrown during command execution", context.exception())
            }
            registerHandler(CommandExecutionException::class.java) { context ->
                context.msg(StandardCaptionKeys.EXCEPTION_UNEXPECTED)
                EsuCore.instance.err("Exception executing command handler", context.exception().cause)
            }

            registerHandler(ArgumentParseException::class.java) { context ->
                context.msg(
                    StandardCaptionKeys.EXCEPTION_INVALID_ARGUMENT,
                    unparsed("cause", context.exception().cause.message)
                )
            }
            registerHandler(NoSuchCommandException::class.java) { context ->
                context.msg(
                    StandardCaptionKeys.EXCEPTION_NO_SUCH_COMMAND,
                    unparsed("command", context.exception().suppliedCommand())
                )
            }
            registerHandler(NoPermissionException::class.java) { context ->
                context.msg(
                    StandardCaptionKeys.EXCEPTION_NO_PERMISSION,
                    unparsed("permission", context.exception().permissionResult().permission().permissionString())
                )
            }
            registerHandler(InvalidCommandSenderException::class.java) { context ->
                val multiple = context.exception().requiredSenderTypes().size != 1
                val expected =
                    if (multiple) context.exception().requiredSenderTypes().joinToString(", ") { type -> TypeUtils.simpleName(type) }
                    else TypeUtils.simpleName(context.exception().requiredSenderTypes().first())

                context.msg(
                    if (multiple) StandardCaptionKeys.EXCEPTION_INVALID_SENDER_LIST
                    else StandardCaptionKeys.EXCEPTION_INVALID_SENDER,

                    unparsed("actual", context.context().sender().javaClass.getSimpleName()),
                    unparsed("expected", expected),
                )

            }
            registerHandler(InvalidSyntaxException::class.java) { context ->
                context.msg(
                    StandardCaptionKeys.EXCEPTION_INVALID_SYNTAX,
                    unparsed("syntax", context.exception().correctSyntax())
                )
            }
        }
    }

    private fun ExceptionContext<out User, out Throwable>.msg(caption: Caption, vararg tags: TagResolver) {
        this.context().sender().minimessage("<ec>" + this.context().formatCaption(caption), *tags)
    }

}