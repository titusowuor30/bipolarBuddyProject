from django import forms
from .models import *

class PrescriptionForm(forms.ModelForm):
    class Meta:
        model = Prescription
        fields = ['patient', 'medication_name', 'dosage']

    def __init__(self, *args, **kwargs):
        doctor = kwargs.pop('doctor', None)
        super(PrescriptionForm, self).__init__(*args, **kwargs)
        if doctor:
            self.fields['patient'].queryset = Patient.objects.filter(doctor=doctor)