package com.lunime.githubcollab.archanaberry.gachadesignstudio
import android.app.Activity

// Objek untuk menyimpan fungsi GachaStudioError.Err404()
object GachaStudioError {
    // Fungsi yang mengembalikan string HTML
    fun Err404(fileName: String): String {
        return """
            <!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, minimal-ui">
  <title>Error Message</title>
  <style>
    body {
      background-color: #000;
      font-family: sans-serif;
      display: flex;
      justify-content: center;
      align-items: center;
      overflow: hidden; /* Menonaktifkan scroll */
      height: 100vh;
      margin: 0; /* Menghilangkan margin default */
      touch-action: none; /* Menonaktifkan zoom dan geser pada layar sentuh */
    }

    .error-box {
      background-color: #311A28;
      color: #ff7777;
      padding: 20px;
      border-radius: 10px;
      border: 10px solid red;
      box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);
    }

    .error-title {
      font-size: 24px;
      font-weight: bold;
      text-align: center;
      margin-bottom: 40px;
    }

	.error-message1 {
      font-size: 18px;
      line-height: 0;
      text-align: center;
    }
    
    .error-message2 {
      font-size: 18px;
      line-height: 1;
      text-align: center;
    }
  
    .error-message3 {
      font-size: 18px;
      line-height: 0;
      text-align: center;
    }
    
  </style>
</head>
<body>
  <div class="error-box">
    <h2 class="error-title">- ERROR -</h2>
    <p class="error-message1">Unable to run game properly!</p>
    <p class="error-message2">The file <b>"$fileName"</b> could not be found!</p>
    <p class="error-message3">Please check the file path, or reinstall and try again.</p>
  </div>
</body>
</html>
        """.trimIndent()
    }
}