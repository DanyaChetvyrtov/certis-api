package ru.digitalhustle.certis.exception.custom

// TODO подумать по поводу подхода, который посоветовал гпт, где каждый
//  ошибки сами знают свои статус коды. Мб получится ещё больше разгрузить
//  обработчики ошибок. Однако я пока хз, как относиться к такому подходу,
//  т.к. логика HTTP ошибки размазывается между моими кастомными ошибками и
//  обработчиком моих кастомных ошибок. Есть наитие, что ломается SRP
abstract class DomainException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
