# Diagnostic de connexion Keycloak - affiche l'erreur reelle sans rien modifier.
# Usage : powershell -ExecutionPolicy Bypass -File .\diag-connexion.ps1

# .NET Framework (Windows PowerShell 5.1) ne propose parfois que TLS 1.0/1.1 par
# defaut, que la plupart des serveurs web modernes refusent desormais.
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

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
    Write-Host ("Type exception : " + $_.Exception.GetType().FullName)
    Write-Host ("Message : " + $_.Exception.Message)
    if ($_.Exception.Response) {
        Write-Host ("Statut HTTP : " + [int]$_.Exception.Response.StatusCode)
    }
    if ($_.ErrorDetails.Message) {
        Write-Host ("Corps reponse : " + $_.ErrorDetails.Message)
    }
}
