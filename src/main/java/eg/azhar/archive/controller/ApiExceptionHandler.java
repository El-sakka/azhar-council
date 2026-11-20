package eg.azhar.archive.controller;

import eg.azhar.archive.dto.ErrorDto;
import eg.azhar.archive.service.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDto notFound(NotFoundException e) {
        return new ErrorDto(e.getMessage());
    }

    @ExceptionHandler({BindException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDto badRequest(Exception e) {
        return new ErrorDto("بيانات الطلب غير صحيحة");
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorDto unauthorized(ServletRequestBindingException e) {
        return new ErrorDto("يجب تسجيل الدخول أولًا");
    }
}
