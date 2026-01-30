from fastapi import FastAPI, File, UploadFile
from transformers import AutoModelForSpeechSeq2Seq, AutoProcessor
import soundfile as sf
import io
import torch 

app = FastAPI()

print("Loading Model")
device = "cuda" if torch.cuda.is_available() else "cpu"

model_name = "openai/whisper-base"
processor = AutoProcessor.from_pretrained(model_name)
model = AutoModelForSpeechSeq2Seq.from_pretrained(model_name).to(device)
print("Model loaded")

@app.get("/health")
def health():
    return {"status": "ok", "device":device}

@app.post("/transcribe")
async def transcribe(audio: UploadFile = File(...)):
    # Read uploaded audio file
    audio_bytes = await audio.read()
    
    # Convert bytes to audio array with sample rate
    audio_data, sample_rate = sf.read(io.BytesIO(audio_bytes))
    
    # Process audio into model format
    inputs = processor(audio_data, sampling_rate=sample_rate, return_tensors="pt").to(device)
    
    # Generate transcription (no gradient needed for inference)
    with torch.no_grad():
        generated_ids = model.generate(**inputs)
    
    # Decode model output to text
    text = processor.batch_decode(generated_ids, skip_special_tokens=True)[0]
    
    return {"text": text}



