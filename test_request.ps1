$body = @{ message = "hello"; chatId = "test" } | ConvertTo-Json
$response = Invoke-WebRequest -Uri "http://localhost:8080/api/chat" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body `
  -UseBasicParsing
Write-Output "Status: $($response.StatusCode)"
Write-Output "Body: $($response.Content)"
