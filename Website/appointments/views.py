# views.py
from django.shortcuts import render,redirect,get_object_or_404
from rest_framework import viewsets
from .models import Appointment
from rest_framework.response import Response
from rest_framework import status
from .serializers import AppointmentSerializer
from rest_framework import permissions
from rest_framework.authentication import BasicAuthentication,TokenAuthentication

def book_appointment(request):
    return render(request,'appointments/book_appointment.html')

def list_appointments(request):
    return render(request,'appointments/list_appointments.html')

class AppointmentViewSet(viewsets.ModelViewSet):
    queryset = Appointment.objects.all()
    serializer_class = AppointmentSerializer
    authentication_classes=[]
    permission_classes=[]

    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        self.perform_create(serializer)
        headers = self.get_success_headers(serializer.data)
        return Response({"detail":"Appointment booked successfully!"}, status=status.HTTP_201_CREATED, headers=headers)
    

def approve_appointment(request, appointment_id):
    appointment = get_object_or_404(Appointment, id=appointment_id)
    if request.method == 'POST':
        appointment.status = 'Approved'
        appointment.save()
        return redirect('doctor_profile')
    return render(request, 'appointments/approve_appointment.html', {'appointment': appointment})

def cancel_appointment(request, appointment_id):
    appointment = get_object_or_404(Appointment, id=appointment_id)
    if request.method == 'POST':
        appointment.status = 'Cancelled'
        appointment.save()
        return redirect('doctor_profile')
    return render(request, 'appointments/cancel_appointment.html', {'appointment': appointment})


