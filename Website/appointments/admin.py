from django.contrib import admin
from .models import Appointment, Departments, Doctor

@admin.register(Appointment)
class AppointmentAdmin(admin.ModelAdmin):
    list_display = ('user','date', 'department', 'doctor', 'message')
    list_filter = ('date', 'department', 'doctor')
    search_fields = ('user', 'message')
    ordering = ('-date',)
    #date_hierarchy = 'date'
    list_per_page = 20

    fieldsets = (
        (None, {
            'fields': ('user',)
        }),
        ('Appointment Details', {
            'fields': ('date', 'department', 'doctor', 'message'),
            'classes': ('collapse',),  # This collapses the details section by default
        }),
    )

