from django.db import models
from accounts.models import Patient, Doctor


class Vitals(models.Model):
    patient = models.ForeignKey(Patient, on_delete=models.CASCADE)
    heart_rate = models.PositiveIntegerField()
    blood_pressure = models.CharField(max_length=50)  # Example format: "120/80"
    o2_saturation = models.PositiveIntegerField()
    respiration_rate = models.PositiveIntegerField()
    tremors = models.PositiveIntegerField()  # Number of tremor episodes
    timestamp = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Notification for {self.patient.name} at {self.timestamp}"

class Prescription(models.Model):
    doctor = models.ForeignKey(Doctor, on_delete=models.CASCADE)
    patient = models.ForeignKey(Patient, on_delete=models.CASCADE)
    medication_name = models.CharField(max_length=100)
    dosage = models.CharField(max_length=100)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Prescription for {self.patient.name} by Dr. {self.doctor.name}"