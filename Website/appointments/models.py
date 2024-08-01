from django.db import models
from accounts.models import Doctor
from accounts.models import Departments
from django.contrib.auth import get_user_model

User=get_user_model()

class Appointment(models.Model):
    user=models.ForeignKey(User, on_delete=models.SET_NULL,null=True,blank=True,related_name='appointments')
    date = models.DateTimeField()
    status=models.CharField(max_length=50,choices=(("Cancelled","Cancelled"),("Pending","Pending"),("Approved","Approved")),default="Pending")
    department = models.ForeignKey(Departments,on_delete=models.SET_NULL,related_name='appointments',null=True,blank=True)
    doctor = models.ForeignKey(Doctor,on_delete=models.CASCADE,related_name='appointments')
    message = models.TextField(blank=True, null=True)

    def __str__(self):
        return f"Appointment for {self.user} with {self.doctor} on {self.date}"
