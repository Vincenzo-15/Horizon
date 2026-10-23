package com.example.demo.controller;

import com.example.demo.dto.UserDTO;
import com.example.demo.model.User;
import com.example.demo.service.AuthService;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        String token = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody User user) {
        UserDTO createdUser = userService.createUser(user);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    // 1. Chiamato dall'app Android: invia il link di reset all'email dell'utente
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            userService.requestPasswordReset(request.getEmail());
            return ResponseEntity.ok(Map.of(
                    "message", "Ti abbiamo inviato un'email con il link per reimpostare la password."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 2. Pagina Web aperta cliccando sul link ricevuto via Email
    @GetMapping(value = "/reset-password-form", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> showResetPasswordPage(@RequestParam String token) {
        try {
            String email = userService.getEmailForResetToken(token);
            String html = """
                <!DOCTYPE html>
                <html lang="it">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Horizon Travel - Reimposta Password</title>
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #F4F6F5; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; padding: 20px; box-sizing: border-box; }
                        .card { background: #FFFFFF; max-width: 420px; width: 100%%; padding: 32px; border-radius: 24px; box-shadow: 0 10px 30px rgba(0,0,0,0.08); }
                        .badge { display: inline-block; background: #E6F7F0; color: #00AA6C; font-weight: 800; font-size: 11px; padding: 5px 12px; border-radius: 50px; letter-spacing: 0.8px; }
                        h2 { color: #1A1A1A; margin: 12px 0 6px 0; font-size: 24px; font-weight: 800; }
                        p { color: #666666; font-size: 14px; margin-bottom: 24px; line-height: 1.5; }
                        label { display: block; font-size: 13px; font-weight: 700; color: #1A1A1A; margin-bottom: 6px; }
                        .input-wrap { position: relative; margin-bottom: 16px; }
                        input[type="password"], input[type="text"] { width: 100%%; padding: 14px; border: 1.5px solid #D0D7DE; border-radius: 14px; font-size: 15px; box-sizing: border-box; outline: none; }
                        input:focus { border-color: #00AA6C; }
                        .toggle { font-size: 12px; color: #00AA6C; font-weight: 700; cursor: pointer; display: inline-block; margin-bottom: 18px; user-select: none; }
                        button { width: 100%%; background: #00AA6C; color: white; border: none; padding: 15px; border-radius: 14px; font-size: 16px; font-weight: 800; cursor: pointer; }
                        button:hover { background: #008f5a; }
                        .hint { font-size: 12px; color: #777; margin-top: -8px; margin-bottom: 16px; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <span class="badge">HORIZON TRAVEL • SICUREZZA</span>
                        <h2>Nuova Password</h2>
                        <p>Stai reimpostando la password per l'account <strong>%s</strong>.</p>
                        <form method="POST" action="/api/auth/reset-password-confirm">
                            <input type="hidden" name="token" value="%s" />
                            <label>Nuova Password</label>
                            <div class="input-wrap">
                                <input type="password" id="pass1" name="newPassword" required placeholder="Almeno 8 caratteri..." />
                            </div>
                            <label>Conferma Nuova Password</label>
                            <div class="input-wrap">
                                <input type="password" id="pass2" name="confirmPassword" required placeholder="Ripeti la password..." />
                            </div>
                            <div class="hint">Richiesti: min. 8 caratteri, 1 maiuscola, 1 numero e 1 simbolo (!@#$%%^&+=!).</div>
                            <span class="toggle" onclick="togglePass()"> Mostra / Nascondi password</span>
                            <button type="submit">Salva Nuova Password</button>
                        </form>
                    </div>
                    <script>
                        function togglePass() {
                            const p1 = document.getElementById('pass1');
                            const p2 = document.getElementById('pass2');
                            const type = p1.type === 'password' ? 'text' : 'password';
                            p1.type = type; p2.type = type;
                        }
                    </script>
                </body>
                </html>
                """.formatted(email, token);
            return ResponseEntity.ok(html);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("""
                <html><body style="font-family:sans-serif;text-align:center;padding:50px;background:#F4F6F5;">
                <div style="background:white;max-width:400px;margin:auto;padding:30px;border-radius:20px;">
                <h2 style="color:#D32F2F;">Link non valido o scaduto</h2>
                <p>%s</p>
                <p>Richiedi un nuovo link dall'app Horizon Travel.</p>
                </div></body></html>
                """.formatted(e.getMessage()));
        }
    }

    // 3. Salva la nuova password inviata dal form HTML della mail
    @PostMapping(value = "/reset-password-confirm", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirmResetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                throw new IllegalArgumentException("Le due password inserite non coincidono.");
            }
            userService.confirmPasswordReset(token, newPassword);
            return ResponseEntity.ok("""
                <html><body style="font-family:sans-serif;text-align:center;padding:50px;background:#F4F6F5;">
                <div style="background:white;max-width:400px;margin:auto;padding:32px;border-radius:24px;box-shadow:0 8px 24px rgba(0,0,0,0.08);">
                <h2 style="color:#00AA6C;"> Password Aggiornata!</h2>
                <p style="color:#333;line-height:1.5;">La tua password è stata modificata con successo. Ora puoi tornare sull'app <strong>Horizon Travel</strong> e accedere.</p>
                </div></body></html>
                """);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("""
                <html><body style="font-family:sans-serif;text-align:center;padding:50px;background:#F4F6F5;">
                <div style="background:white;max-width:400px;margin:auto;padding:32px;border-radius:24px;">
                <h2 style="color:#D32F2F;">Attenzione</h2>
                <p style="color:#333;">%s</p>
                <a href="/api/auth/reset-password-form?token=%s" style="color:#00AA6C;font-weight:bold;">← Torna indietro e riprova</a>
                </div></body></html>
                """.formatted(e.getMessage(), token));
        }
    }

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data
    public static class ForgotPasswordRequest {
        private String email;
    }
}