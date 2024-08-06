from django.shortcuts import render, redirect,get_object_or_404
from django.contrib.auth import login, authenticate,logout
from django.contrib.auth.forms import AuthenticationForm
from .forms import CustomUserCreationForm, LoginForm
from django.contrib import messages

from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import AllowAny

from django.contrib.auth.views import LoginView
from django.urls import reverse_lazy
from .forms import CustomAuthenticationForm
from rest_framework import viewsets, status
from rest_framework.response import Response
from rest_framework import permissions
from .models import *
from appointments.models import *
from core.models import *
from .serializers import *
from django.contrib.auth.models import Group

def doctors(request):
    return render(request,'accounts/doctors.html')

#android login endpoint
class LoginAPIView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        form = LoginForm(request, data=request.data)
        if form.is_valid():
            email = form.cleaned_data.get('username')
            password = form.cleaned_data.get('password')
            user = authenticate(request, username=email, password=password)
            if user is not None:
                login(request, user)
                messages.success(request, "Login successful!")
                return Response({'success': True, 'message': 'Login successful!'}, status=status.HTTP_200_OK)
            else:
                return Response({'success': False, 'message': 'Login failed. Please check credentials and try again!'}, status=status.HTTP_401_UNAUTHORIZED)
        else:
            return Response({'success': False, 'errors': form.errors}, status=status.HTTP_400_BAD_REQUEST)

class LoginView(LoginView):
    form_class = CustomAuthenticationForm
    template_name = 'accounts/login.html'
    success_url = reverse_lazy('home')

    def form_valid(self, form):
        messages.success(self.request, "You have successfully logged in.")
        return super().form_valid(form)

    def form_invalid(self, form):
        messages.error(self.request, "Invalid email or password. Please try again.")
        return super().form_invalid(form)

def signup_view(request):
    if request.method == 'POST':
        form = CustomUserCreationForm(request.POST)
        if form.is_valid():
            messages.success(request,"Your account has been created successfully!")
            user = form.save()
            login(request, user)
            return redirect('home')  # Redirect to a success page
    else:
        form = CustomUserCreationForm()
    return render(request, 'accounts/signup.html', {'form': form})

def log_out(request):
    logout(request)
    return redirect('login')


class PatientSignupViewSet(viewsets.ModelViewSet):
    queryset = Patient.objects.all()
    serializer_class = PatientSerializer
    authentication_classes = []
    permission_classes = []

    def create(self, request, *args, **kwargs):
        print(request.data)
        user_data = {
            'username': request.data.get('username'),
            'first_name': request.data.get('first_name'),
            'last_name': request.data.get('last_name'),
            'email': request.data.get('email'),
            'password': request.data.get('password'),
            'gender': request.data.get('gender'),
            'phone': request.data.get('phone'),
            'age': request.data.get('age'),
            'height': request.data.get('height'),
            'weight': request.data.get('weight')
        }

        # Create a new CustomUser instance
        user_serializer = CustomUserSerializer(data=user_data)
        user_serializer.is_valid(raise_exception=True)
        user = user_serializer.save()
        pg, created = Group.objects.get_or_create(name='patients')
        user.groups.add(pg)
        user.save()

        # Ensure 'doctor' field exists and is a valid reference
        # doctor_id = request.data.get('doctor')
        # if not doctor_id:
        #     return Response({'detail': 'Doctor ID is required.'}, status=status.HTTP_400_BAD_REQUEST)

        # try:
        #     doctor = Doctor.objects.get(id=doctor_id)
        # except Doctor.DoesNotExist:
        #     return Response({'detail': 'Invalid doctor ID.'}, status=status.HTTP_400_BAD_REQUEST)

        # Create the Patient instance using the newly created CustomUser
        patient_data = {
            'user': user.id,  # Link to the created CustomUser
            'doctor':None  # Ensure this field exists in the request
        }

        patient_serializer = self.get_serializer(data=patient_data)
        patient_serializer.is_valid(raise_exception=True)
        self.perform_create(patient_serializer)

        headers = self.get_success_headers(patient_serializer.data)
        return Response(patient_serializer.data, status=status.HTTP_201_CREATED, headers=headers)
    

    class DoctorViewSet(viewsets.ReadOnlyModelViewSet):
        queryset = Doctor.objects.all()
        serializer_class = DoctorSerializer
        permission_classes = []
        authentication_classes = []
    
class DoctorViewSet(viewsets.ModelViewSet):
    queryset = Doctor.objects.all()
    serializer_class = DoctorSerializer
    permission_classes = []
    authentication_classes = []

    def get_queryset(self):
        queryset = super().get_queryset()
        department_id = self.request.query_params.get('department')
        if department_id:
            queryset = queryset.filter(id=department_id)
        return queryset
    

def patient_profile(request):
    user = request.user
    appointments = Appointment.objects.filter(user=user).order_by('date')
    
    context = {
        'user': user,
        'appointments': appointments
    }
    return render(request, 'accounts/patient_profile.html', context)

def doctor_profile(request):
    user = request.user
    doctor = get_object_or_404(Doctor, user=user)
    patients = Patient.objects.filter(doctor=doctor)
    vitals = Vitals.objects.filter(patient__in=patients)
    tremors = Tremors.objects.filter(patient__in=patients)
    prescriptions = Prescription.objects.filter(patient__in=patients)
    appointments = Appointment.objects.filter(doctor=doctor)
    context = {
        'user': user,
        'doctor': doctor,
        'patients': patients,
        'vitals': vitals,
        'tremors': tremors,
        'prescriptions': prescriptions,
        'appointments': appointments
    }
    return render(request, 'accounts/doctor_profile.html', context)

def kin_profile(request):
    user = request.user
    patient_kin = get_object_or_404(PatientKin, user=user)
    patient = patient_kin.patient
    vitals = Vitals.objects.filter(patient=patient)
    tremors = Tremors.objects.filter(patient=patient)
    prescriptions = Prescription.objects.filter(patient=patient)
    context = {
        'user': user,
        'vitals': vitals,
        'tremors': tremors,
        'prescriptions': prescriptions
    }
    return render(request, 'accounts/kin_profile.html', context)

