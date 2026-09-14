# Diagnostic de connexion Keycloak - affiche l'erreur reelle sans rien modifier.
# Usage : powershell -ExecutionPolicy Bypass -File .\diag-connexion.ps1

$user = Read-Host "Identifiant"
$passSecure = Read-Host "Mot de passe" -AsSecureString
$pass = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($passSecure)
)

try {
    $resp = Invoke-RestMethod -Uri "https://auth.asce-lc.bf/realms/asce-lc-realm/protocol/openid-connect/token" `
        -Method Post `
        -Body @{ grant_type = "password"; client_id = "agenda-cge"; username = $user; password = $pass; scope = "openid" } `
        -ContentType "application/x-www-form-urlencoded"
    Write-Host "SUCCES : token recu." -ForegroundColor Green
} catch {
    Write-Host "ECHEC :" -ForegroundColor Red
    Write-Host $_.ErrorDetails.Message
}
