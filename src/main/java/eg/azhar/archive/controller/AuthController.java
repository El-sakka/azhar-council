package eg.azhar.archive.controller;

import eg.azhar.archive.config.SessionKeys;
import eg.azhar.archive.dto.ErrorDto;
import eg.azhar.archive.dto.LoginRequest;
import eg.azhar.archive.dto.UserDto;
import eg.azhar.archive.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        return auth.authenticate(body.username(), body.password())
                .<ResponseEntity<?>>map(user -> {
                    HttpSession old = request.getSession(false);
                    if (old != null) old.invalidate();          // prevent session fixation
                    request.getSession(true).setAttribute(SessionKeys.USER, user);
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorDto("اسم المستخدم أو كلمة المرور غير صحيحة")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserDto me(@SessionAttribute(SessionKeys.USER) UserDto user) {
        return user;
    }
}
