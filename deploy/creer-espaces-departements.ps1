# Cree les 7 espaces departementaux ASCE-LC sur la VM (agenda.asce-lc.bf).
# A executer TOI-MEME dans PowerShell (pas via l'assistant), depuis n'importe
# quelle machine ayant acces a auth.asce-lc.bf et agenda.asce-lc.bf en HTTPS.
#
# Usage interactif (recommande - mot de passe masque, pas dans l'historique) :
#   .\creer-espaces-departements.ps1
#
# Usage non interactif (le mot de passe reste alors visible dans l'historique
# PowerShell et dans la liste des processus le temps de l'execution) :
#   .\creer-espaces-departements.ps1 -Username admin.cge -Password "MonMotDePasse"

param(
    [string]$Username,
    [string]$Password
)

$AuthUrl   = "https://auth.asce-lc.bf/realms/asce-lc-realm/protocol/openid-connect/token"
$ApiUrl    = "https://agenda.asce-lc.bf/api/v1/cge-agenda/admin/espaces"
$ClientId  = "agenda-cge"

if ($Username) {
    $KcUser = $Username
} else {
    $KcUser = Read-Host "Identifiant Keycloak (compte ADMIN)"
}

if ($Password) {
    $KcPass = $Password
} else {
    $KcPassSecure = Read-Host "Mot de passe" -AsSecureString
    $KcPass = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($KcPassSecure)
    )
}

$tokenBody = @{
    grant_type = "password"
    client_id  = $ClientId
    username   = $KcUser
    password   = $KcPass
    scope      = "openid"
}

try {
    $tokenResp = Invoke-RestMethod -Uri $AuthUrl -Method Post -Body $tokenBody -ContentType "application/x-www-form-urlencoded"
} catch {
    Write-Host "Echec de connexion : identifiant/mot de passe incorrect, ou compte sans droit ADMIN." -ForegroundColor Red
    exit 1
}

$KcPass = $null
$Token = $tokenResp.access_token

if (-not $Token) {
    Write-Host "Echec : aucun token recu." -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $Token"
    "Content-Type"  = "application/json; charset=utf-8"
}

function New-Espace($nom, $email) {
    Write-Host "=== $nom ($email) ===" -ForegroundColor Cyan
    $body = @{ nom = $nom; chefEmail = $email; chefNom = "À affecter" } | ConvertTo-Json -Compress
    try {
        $resp = Invoke-RestMethod -Uri $ApiUrl -Method Post -Headers $headers -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
        Write-Host "OK -> id=$($resp.id)" -ForegroundColor Green
    } catch {
        Write-Host "ECHEC : $($_.Exception.Message)" -ForegroundColor Red
        if ($_.ErrorDetails.Message) { Write-Host $_.ErrorDetails.Message }
    }
    Write-Host ""
}

New-Espace "Département d'Audit et de Contrôle" "audit-controle@asce-lc.bf"
New-Espace "Département d'Enquête et d'Investigation" "enquete-investigation@asce-lc.bf"
New-Espace "Département des Déclarations d'Intérêt et de Patrimoine" "declarations-patrimoine@asce-lc.bf"
New-Espace "Département de la Stratégie Nationale de la Prévention" "strategie-prevention@asce-lc.bf"
New-Espace "Département du Suivi des Recommandations et des Actions en Justice" "suivi-recommandations@asce-lc.bf"
New-Espace "Secrétariat Général" "secretariat-general@asce-lc.bf"
New-Espace "Contrôleur Général d'État et Contrôleur Général d'État Adjoint" "cge-adjoint@asce-lc.bf"

Write-Host "Termine. Verifie sur /admin/espaces que les 7 espaces sont bien crees avant"
Write-Host "de remplacer les emails placeholder par les vraies adresses des chefs."
