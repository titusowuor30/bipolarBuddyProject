from django.db import models
from accounts.models import Patient, Doctor


class Vitals(models.Model):
    patient = models.ForeignKey(Patient, on_delete=models.CASCADE)
    heart_rate = models.CharField(max_length=10,default="")
    blood_pressure = models.CharField(max_length=50,default="")  # Example format: "120/80"
    o2_saturation = models.CharField(max_length=10,blank=True,null=True,default="")
    respiration_rate = models.CharField(max_length=10,default="")
    timestamp = models.DateTimeField(auto_now_add=True)

    class Meta:
        verbose_name_plural='Vitals'

    def __str__(self):
        return f"Notification for {self.patient.user.email} at {self.timestamp}"
    
    
class Tremors(models.Model):
    patient = models.ForeignKey(Patient, on_delete=models.CASCADE)
    timestamp = models.DateTimeField(auto_now_add=True)

    class Meta:
        verbose_name_plural='Tremors'

    def __str__(self):
        return f"Tremor - {self.patient.user.email} at {self.timestamp}"

class Prescription(models.Model):
    doctor = models.ForeignKey(Doctor, on_delete=models.CASCADE)
    patient = models.ForeignKey(Patient, on_delete=models.CASCADE)
    medication_name = models.CharField(max_length=100)
    dosage = models.CharField(max_length=100)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        verbose_name_plural='Prescriptions'

    def __str__(self):
        return f"Prescription for {self.patient.user.email} by Dr. {self.doctor.user.email}"