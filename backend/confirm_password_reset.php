<?php
header("Content-Type: application/json");
require_once "db.php";
require_once __DIR__ . "/input_validation.php";

function respond($status, $success, $message) {
    http_response_code($status);
    echo json_encode(["success" => $success, "message" => $message]);
    exit;
}

$data = vetschedReadJsonRequest();
$email = $data === null ? null : vetschedNormalizeEmail($data["email"] ?? null);
$code = $data["code"] ?? null;
$password = $data["password"] ?? null;

if ($email === null || !vetschedIsValidOtp($code)) {
    respond(400, false, "Enter the registered email address and 6-digit code");
}
if (!vetschedIsValidPassword($password)) {
    respond(400, false, "Password must contain 10 to 128 characters");
}

try {
    $conn->beginTransaction();
    $lookup = $conn->prepare(
        "SELECT code_hash, attempts, expires_at > CURRENT_TIMESTAMP AS is_valid
         FROM password_reset_otps WHERE email = :email FOR UPDATE"
    );
    $lookup->execute([":email" => $email]);
    $otp = $lookup->fetch();

    if (!$otp) {
        $conn->rollBack();
        respond(400, false, "No active reset code was found. Request a new code.");
    }
    if ((int) $otp["attempts"] >= 5) {
        $conn->rollBack();
        respond(429, false, "Too many incorrect attempts. Request a new code.");
    }
    if (!in_array($otp["is_valid"], [true, "t", "1", 1], true)) {
        $deleteExpired = $conn->prepare("DELETE FROM password_reset_otps WHERE email = :email");
        $deleteExpired->execute([":email" => $email]);
        $conn->commit();
        respond(400, false, "The reset code expired. Request a new code.");
    }
    if (!password_verify($code, $otp["code_hash"])) {
        $increment = $conn->prepare(
            "UPDATE password_reset_otps SET attempts = attempts + 1 WHERE email = :email"
        );
        $increment->execute([":email" => $email]);
        $conn->commit();
        respond(400, false, "The reset code is incorrect.");
    }

    $update = $conn->prepare(
        "UPDATE account SET password_hash = :password_hash WHERE LOWER(email) = :email"
    );
    $update->execute([
        ":password_hash" => password_hash($password, PASSWORD_DEFAULT),
        ":email" => $email,
    ]);
    if ($update->rowCount() !== 1) {
        $conn->rollBack();
        respond(400, false, "Account not found. Request a new code.");
    }

    $delete = $conn->prepare("DELETE FROM password_reset_otps WHERE email = :email");
    $delete->execute([":email" => $email]);
    $conn->commit();
    respond(200, true, "Password reset successfully. You can now log in.");
} catch (Throwable $e) {
    if ($conn->inTransaction()) {
        $conn->rollBack();
    }
    error_log("Password reset confirmation failed: " . $e->getMessage());
    respond(500, false, "Could not reset the password. Please try again later.");
}
?>
