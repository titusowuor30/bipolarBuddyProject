from django.contrib.auth.hashers import make_password
from django.db import models
from django.contrib.auth.models import BaseUserManager, AbstractUser

class CustomUserManager(BaseUserManager):
    def create_user(self, email, phone, password=None, **extra_fields):
        """
        Creates and saves a User with the given email, phone, and password.
        """
       
        if not email:
            raise ValueError("Users must have an email address")

        user = self.model(
            email=self.normalize_email(email),
            phone=phone,
            **extra_fields
        )
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, email, phone, password=None, **extra_fields):
        extra_fields.setdefault('is_staff', True)
        extra_fields.setdefault('is_superuser', True)

        if extra_fields.get('is_staff') is not True:
            raise ValueError('Superuser must have is_staff=True.')
        if extra_fields.get('is_superuser') is not True:
            raise ValueError('Superuser must have is_superuser=True.')

        return self.create_user(email, phone, password=password, **extra_fields)

class CustomUser(AbstractUser):
    GENDER = [("M", "Male"), ("F", "Female"), ("Other", "Other")]
    email = models.EmailField(
        verbose_name='Email Address',
        max_length=100,
        unique=True,
    )
    gender = models.CharField(max_length=10, choices=GENDER, default='Other')
    phone = models.CharField(max_length=15, blank=True, null=True)
    age = models.IntegerField(default=18,blank=True, null=True)
    height = models.IntegerField(max_length=8, blank=True, null=True)
    weight = models.IntegerField(max_length=8,blank=True, null=True)

    objects = CustomUserManager()

    #USERNAME_FIELD = "email"
    REQUIRED_FIELDS = ['phone', 'first_name', 'last_name']

    def __str__(self):
        return self.email

class Doctor(models.Model):
    SPECIALIZATIONS=[("Pyschiatrist","Pyschiatrist"), ("Clinical Pyschologist","Clinical Pyschologist")]
    user=models.OneToOneField("CustomUser",on_delete=models.CASCADE)
    #contact_info=models.ManyToManyField("ContactInfo", blank=True, null=True)
    specialization=models.CharField(max_length=50, choices=SPECIALIZATIONS)

    def __str__(self):
        return f"{self.user.first_name} {self.user.last_name} - {self.specialization}"
    
class  Patient(models.Model):
    doctor=models.ForeignKey("Doctor", on_delete=models.SET_NULL, related_name="patients", null=True)
    user=models.OneToOneField("CustomUser", on_delete=models.CASCADE)

    def __str__(self):
        return f"{self.user.first_name} {self.user.last_name} - {self.doctor.user.first_name}"

class PatientKin(models.Model):
    RELATIONSHIPS=[("Sp", "Spouse"), ("Sb","Sibling"), ("P", "Parent"), ("Gp","GrandParent"), ("Other", "Other")]
    patient=models.ForeignKey("Patient", on_delete=models.CASCADE, related_name="kins")
    user=models.OneToOneField("CustomUser", on_delete=models.CASCADE)
    relation=models.CharField(max_length=30, choices=RELATIONSHIPS)
    
    def __str__(self):
        return f"{self.user.first_name} {self.user.last_name} - {self.patient.user.first_name}"