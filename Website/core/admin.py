from django.contrib import admin
from .models import *
from accounts.models import Departments

# Register your models here.
@admin.register(Departments)
class DepartmentAdmin(admin.ModelAdmin):
    list_display=['name','short_description']

@admin.register(Vitals)
class ViotalsAdmin(admin.ModelAdmin):
    list_display=['patient','heart_rate','blood_pressure','o2_saturation','respiration_rate','respiration_rate','timestamp']

@admin.register(Tremors)
class TremorsAdmin(admin.ModelAdmin):
    list_display=['patient','timestamp']

@admin.register(Prescription)
class PrescriptionAdmin(admin.ModelAdmin):
    list_display=['doctor','patient','medication_name','dosage','created_at']



