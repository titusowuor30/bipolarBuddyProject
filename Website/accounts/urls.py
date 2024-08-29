from django.urls import path,include
from .views import *
from rest_framework.routers import DefaultRouter

router = DefaultRouter()
router.register(r'patients/signup', PatientSignupViewSet, basename='patient-signup')
router.register(r'doctors', DoctorViewSet)


urlpatterns = [
    path('api/', include(router.urls)),
    path('signup/', signup_view, name='signup'),
    path('logout/',log_out, name='logout'),
    path('login/', LoginView.as_view(), name='login'),
    path('api/login/', LoginAPIView.as_view(), name='login_api'),
    path('doctors/', doctors, name='doctors'),
    path('patient-profile/', patient_profile, name='patient_profile'),
    path('doctor-profile/', doctor_profile, name='doctor_profile'),
    path('kin-profile/', kin_profile, name='kin_profile'),
    path('add-prescription/', add_prescription, name='add_prescription'),
    path('edit-prescription/<int:pk>/', edit_prescription, name='edit_prescription'),
    path('delete-prescription/<int:pk>/', delete_prescription, name='delete_prescription'),
]
