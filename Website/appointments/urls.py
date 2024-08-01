# urls.py

from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import *

router = DefaultRouter()
router.register(r'appointments', AppointmentViewSet)


urlpatterns = [
    path('api/', include(router.urls)),
    path("appointments/book/",book_appointment,name='book_appointment'),
    path("appointments/list/",list_appointments,name='list_appointments'),
    path('approve-appointment/<int:appointment_id>/', approve_appointment, name='approve_appointment'),
    path('cancel-appointment/<int:appointment_id>/', cancel_appointment, name='cancel_appointment'),
]
